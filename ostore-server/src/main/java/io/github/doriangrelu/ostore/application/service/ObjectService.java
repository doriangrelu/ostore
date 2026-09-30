/*
 * Copyright 2026 the OStore contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.doriangrelu.ostore.application.service;

import io.github.doriangrelu.ostore.application.command.CopyObjectCommand;
import io.github.doriangrelu.ostore.application.command.CreateObjectCommand;
import io.github.doriangrelu.ostore.application.command.ReplaceObjectCommand;
import io.github.doriangrelu.ostore.application.command.TransactionMode;
import io.github.doriangrelu.ostore.application.io.MeteredInputStream;
import io.github.doriangrelu.ostore.application.port.in.ObjectUseCases;
import io.github.doriangrelu.ostore.application.port.out.BlobPurgeQueue;
import io.github.doriangrelu.ostore.application.port.out.BucketRepository;
import io.github.doriangrelu.ostore.application.port.out.ObjectRepository;
import io.github.doriangrelu.ostore.application.port.out.StorageDrivers;
import io.github.doriangrelu.ostore.application.port.out.TransactionRepository;
import io.github.doriangrelu.ostore.application.port.out.UnitOfWork;
import io.github.doriangrelu.ostore.application.result.ObjectContent;
import io.github.doriangrelu.ostore.application.result.ObjectPage;
import io.github.doriangrelu.ostore.domain.exception.BucketNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.ObjectLockedException;
import io.github.doriangrelu.ostore.domain.exception.ObjectNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.TransactionNotFoundException;
import io.github.doriangrelu.ostore.domain.model.Bucket;
import io.github.doriangrelu.ostore.domain.model.StoredObject;
import io.github.doriangrelu.ostore.domain.model.Transaction;
import io.github.doriangrelu.ostore.domain.model.vo.BlobLocation;
import io.github.doriangrelu.ostore.domain.model.vo.BucketName;
import io.github.doriangrelu.ostore.domain.model.vo.RangeRequest;
import io.github.doriangrelu.ostore.domain.model.vo.TransactionPolicy;
import io.github.doriangrelu.ostore.domain.util.Identifiers;
import io.github.doriangrelu.ostore.driver.spi.model.BlobPath;
import io.github.doriangrelu.ostore.driver.spi.model.ByteRange;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Implémentation des cas d'usage des objets.
 *
 * <p>Ordre des écritures (ADR-0006) : le contenu est écrit <b>d'abord</b>, dans un blob neuf, puis les
 * métadonnées sont enregistrées dans une {@link UnitOfWork}. Si l'une des deux étapes échoue, le blob orphelin
 * est planifié pour purge.
 *
 * <p>Écritures transactionnelles (ADR-0016) : la transaction rejointe est vérifiée avant d'écrire le blob (échec
 * rapide), puis <b>verrouillée</b> au moment d'enregistrer l'objet, pour qu'aucune écriture ne la rejoigne
 * pendant sa clôture.
 */
public final class ObjectService implements ObjectUseCases {

    private final BucketRepository buckets;
    private final ObjectRepository objects;
    private final TransactionRepository transactions;
    private final BlobPurgeQueue purgeQueue;
    private final StorageDrivers drivers;
    private final UnitOfWork unitOfWork;
    private final TransactionPolicy policy;
    private final Clock clock;

    public ObjectService(
            BucketRepository buckets,
            ObjectRepository objects,
            TransactionRepository transactions,
            BlobPurgeQueue purgeQueue,
            StorageDrivers drivers,
            UnitOfWork unitOfWork,
            TransactionPolicy policy,
            Clock clock) {
        this.buckets = buckets;
        this.objects = objects;
        this.transactions = transactions;
        this.purgeQueue = purgeQueue;
        this.drivers = drivers;
        this.unitOfWork = unitOfWork;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public StoredObject create(CreateObjectCommand command) {
        var bucket = bucketOf(command.bucket());
        checkBeforeWriting(command.transaction());
        var now = clock.instant();
        var content =
                writeContent(bucket.driverId(), command.content(), command.contentLength(), command.contentType(), now);
        return persistOrPurge(content.blob(), () -> {
            var transactionId = join(command.transaction(), now);
            return objects.insert(
                    StoredObject.create(bucket.id(), command.name(), content, command.metadata(), transactionId, now));
        });
    }

    @Override
    public StoredObject replace(ReplaceObjectCommand command) {
        var target = get(command.id(), null);
        checkBeforeWriting(command.transaction());
        var now = clock.instant();
        var content = writeContent(
                target.blob().driverId(), command.content(), command.contentLength(), command.contentType(), now);
        return persistOrPurge(content.blob(), () -> replaceLocked(command, content, now));
    }

    @Override
    public StoredObject copy(CopyObjectCommand command) {
        var source = get(command.sourceId(), null);
        var target = Optional.ofNullable(command.targetBucket()).map(this::bucketOf);
        var bucketId = target.map(Bucket::id).orElse(source.bucketId());
        var driverId = target.map(Bucket::driverId).orElse(source.blob().driverId());
        checkBeforeWriting(command.transaction());
        var now = clock.instant();
        StoredObject.Content content;
        try (var sourceContent = read(source.blob(), Optional.empty())) {
            content = writeContent(driverId, sourceContent, source.size(), source.contentType(), now);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var name = command.name() != null ? command.name() : source.name();
        return persistOrPurge(content.blob(), () -> {
            var transactionId = join(command.transaction(), now);
            return objects.insert(StoredObject.create(bucketId, name, content, source.metadata(), transactionId, now));
        });
    }

    @Override
    public StoredObject get(UUID id, @Nullable UUID transactionId) {
        var object = objects.find(id)
                .filter(found -> found.replaces() == null)
                .orElseThrow(() -> new ObjectNotFoundException(id));
        if (transactionId == null) {
            return object;
        }
        return objects.findPendingReplacement(id)
                .filter(replacement -> transactionId.equals(replacement.transactionId()))
                .orElse(object);
    }

    @Override
    public ObjectContent open(UUID id, Optional<RangeRequest> range, @Nullable UUID transactionId) {
        var object = get(id, transactionId);
        var resolved = range.map(request -> request.resolve(object.size()));
        var stream = read(object.blob(), resolved.map(r -> new ByteRange(r.start(), r.end())));
        return new ObjectContent(object, stream, resolved);
    }

    @Override
    public ObjectPage list(BucketName bucket, @Nullable String namePrefix, Optional<UUID> after, int limit) {
        int pageSize = Math.clamp(limit, 1, MAX_PAGE_SIZE);
        // Un élément de plus que la page : sa présence indique qu'il reste des objets.
        return ObjectPage.of(objects.list(bucketOf(bucket).id(), namePrefix, after, pageSize + 1), pageSize);
    }

    @Override
    public void delete(UUID id) {
        var object = get(id, null);
        objects.findPendingReplacement(id).ifPresent(replacement -> {
            throw new ObjectLockedException(id, Objects.requireNonNull(replacement.transactionId()));
        });
        objects.delete(object);
    }

    /**
     * Remplacement, sous verrou de la transaction concernée :
     *
     * <ul>
     *   <li>objet en attente : corrigé sur place, dans sa propre transaction (qui doit être ouverte) ;
     *   <li>objet validé, hors transaction : remplacé directement, sauf remplacement en attente ;
     *   <li>objet validé, en transaction : version en attente créée, ou mise à jour si la même transaction en a
     *       déjà une.
     * </ul>
     */
    private StoredObject replaceLocked(ReplaceObjectCommand command, StoredObject.Content content, Instant now) {
        var current = objects.find(command.id()).orElseThrow(() -> new ObjectNotFoundException(command.id()));
        if (current.isPending()) {
            var ownTransaction = Objects.requireNonNull(current.transactionId());
            var requested = command.transaction();
            if (requested instanceof TransactionMode.Pending
                    || (requested instanceof TransactionMode.Join(UUID joined) && !joined.equals(ownTransaction))) {
                throw new ObjectLockedException(current.id(), ownTransaction);
            }
            lockOpen(ownTransaction, now);
            return objects.replace(current.withContent(content, command.name(), command.metadata(), now));
        }
        var pending = objects.findPendingReplacement(current.id());
        var transactionId = join(command.transaction(), now);
        if (transactionId == null) {
            pending.ifPresent(replacement -> {
                throw new ObjectLockedException(current.id(), Objects.requireNonNull(replacement.transactionId()));
            });
            return objects.replace(current.withContent(content, command.name(), command.metadata(), now));
        }
        if (pending.isPresent()) {
            var replacement = pending.get();
            if (!transactionId.equals(replacement.transactionId())) {
                throw new ObjectLockedException(current.id(), Objects.requireNonNull(replacement.transactionId()));
            }
            return objects.replace(replacement.withContent(content, command.name(), command.metadata(), now));
        }
        return objects.insert(
                current.pendingReplacement(content, command.name(), command.metadata(), transactionId, now));
    }

    /** Échec rapide avant d'écrire un blob : transaction rejointe ouverte, durée demandée valide. */
    private void checkBeforeWriting(TransactionMode mode) {
        switch (mode) {
            case TransactionMode.None _ -> {}
            case TransactionMode.Join(UUID id) ->
                transactions
                        .find(id)
                        .orElseThrow(() -> new TransactionNotFoundException(id))
                        .ensureOpen(clock.instant());
            case TransactionMode.Pending(var ttl) -> policy.resolve(ttl);
        }
    }

    /**
     * Transaction de l'écriture, à appeler dans la {@link UnitOfWork} : la transaction rejointe est verrouillée et
     * doit être ouverte ; en mode implicite, une transaction est créée.
     *
     * @return identifiant de la transaction, {@code null} hors transaction
     */
    private @Nullable UUID join(TransactionMode mode, Instant now) {
        return switch (mode) {
            case TransactionMode.None _ -> null;
            case TransactionMode.Join(UUID id) -> lockOpen(id, now).id();
            case TransactionMode.Pending(var ttl) ->
                transactions
                        .insert(Transaction.open(null, policy.resolve(ttl), now))
                        .id();
        };
    }

    private Transaction lockOpen(UUID transactionId, Instant now) {
        var transaction =
                transactions.lock(transactionId).orElseThrow(() -> new TransactionNotFoundException(transactionId));
        transaction.ensureOpen(now);
        return transaction;
    }

    private Bucket bucketOf(BucketName name) {
        return buckets.findByName(name).orElseThrow(() -> new BucketNotFoundException(name));
    }

    private InputStream read(BlobLocation location, Optional<ByteRange> range) {
        return drivers.driver(location.driverId()).read(new BlobPath(location.path()), range);
    }

    /**
     * Écrit un contenu dans un blob neuf, rangé selon la stratégie de chemins du driver, et vérifie sa taille.
     * En cas d'échec, le blob éventuellement écrit est planifié pour purge.
     */
    private StoredObject.Content writeContent(
            String driverId, InputStream content, long expectedSize, @Nullable String contentType, Instant now) {
        var path = drivers.layout(driverId).pathOf(Identifiers.newId(now), now);
        var location = new BlobLocation(driverId, path.value());
        var metered = new MeteredInputStream(content);
        try {
            drivers.driver(driverId).write(path, metered, expectedSize);
            metered.requireExactly(expectedSize);
        } catch (RuntimeException e) {
            purgeQueue.schedule(location);
            throw e;
        }
        return new StoredObject.Content(
                location,
                metered.count(),
                metered.md5Hex(),
                contentType != null ? contentType : StoredObject.DEFAULT_CONTENT_TYPE);
    }

    /** Enregistre les métadonnées dans une unité de travail ; en cas d'échec, le blob écrit est purgé. */
    private StoredObject persistOrPurge(BlobLocation written, Supplier<StoredObject> persistence) {
        try {
            return unitOfWork.execute(persistence);
        } catch (RuntimeException e) {
            purgeQueue.schedule(written);
            throw e;
        }
    }
}
