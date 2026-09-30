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
import io.github.doriangrelu.ostore.application.io.MeteredInputStream;
import io.github.doriangrelu.ostore.application.port.in.ObjectUseCases;
import io.github.doriangrelu.ostore.application.port.out.BlobPurgeQueue;
import io.github.doriangrelu.ostore.application.port.out.BucketRepository;
import io.github.doriangrelu.ostore.application.port.out.ObjectRepository;
import io.github.doriangrelu.ostore.application.port.out.StorageDrivers;
import io.github.doriangrelu.ostore.application.result.ObjectContent;
import io.github.doriangrelu.ostore.application.result.ObjectPage;
import io.github.doriangrelu.ostore.domain.exception.BucketNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.ObjectNotFoundException;
import io.github.doriangrelu.ostore.domain.model.Bucket;
import io.github.doriangrelu.ostore.domain.model.StoredObject;
import io.github.doriangrelu.ostore.domain.model.vo.BlobLocation;
import io.github.doriangrelu.ostore.domain.model.vo.BucketName;
import io.github.doriangrelu.ostore.domain.model.vo.RangeRequest;
import io.github.doriangrelu.ostore.domain.util.Identifiers;
import io.github.doriangrelu.ostore.driver.spi.model.BlobPath;
import io.github.doriangrelu.ostore.driver.spi.model.ByteRange;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Implémentation des cas d'usage des objets.
 *
 * <p>Ordre des écritures (ADR-0006) : le contenu est écrit <b>d'abord</b>, dans un blob neuf, puis les
 * métadonnées sont enregistrées. Si l'une des deux étapes échoue, le blob orphelin est planifié pour purge :
 * aucune donnée n'est perdue ni laissée à l'abandon.
 */
public final class ObjectService implements ObjectUseCases {

    private final BucketRepository buckets;
    private final ObjectRepository objects;
    private final BlobPurgeQueue purgeQueue;
    private final StorageDrivers drivers;
    private final Clock clock;

    public ObjectService(
            BucketRepository buckets,
            ObjectRepository objects,
            BlobPurgeQueue purgeQueue,
            StorageDrivers drivers,
            Clock clock) {
        this.buckets = buckets;
        this.objects = objects;
        this.purgeQueue = purgeQueue;
        this.drivers = drivers;
        this.clock = clock;
    }

    @Override
    public StoredObject create(CreateObjectCommand command) {
        var bucket = bucketOf(command.bucket());
        var now = clock.instant();
        var content =
                writeContent(bucket.driverId(), command.content(), command.contentLength(), command.contentType(), now);
        return insertOrPurge(StoredObject.create(bucket.id(), command.name(), content, command.metadata(), now));
    }

    @Override
    public StoredObject replace(ReplaceObjectCommand command) {
        var current = get(command.id());
        var now = clock.instant();
        var content = writeContent(
                current.blob().driverId(), command.content(), command.contentLength(), command.contentType(), now);
        var updated = current.withContent(content, command.name(), command.metadata(), now);
        try {
            return objects.replace(updated);
        } catch (RuntimeException e) {
            purgeQueue.schedule(content.blob());
            throw e;
        }
    }

    @Override
    public StoredObject copy(CopyObjectCommand command) {
        var source = get(command.sourceId());
        var target = Optional.ofNullable(command.targetBucket()).map(this::bucketOf);
        var bucketId = target.map(Bucket::id).orElse(source.bucketId());
        var driverId = target.map(Bucket::driverId).orElse(source.blob().driverId());
        var now = clock.instant();
        StoredObject.Content content;
        try (var sourceContent = read(source.blob(), Optional.empty())) {
            content = writeContent(driverId, sourceContent, source.size(), source.contentType(), now);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var name = command.name() != null ? command.name() : source.name();
        return insertOrPurge(StoredObject.create(bucketId, name, content, source.metadata(), now));
    }

    @Override
    public StoredObject get(UUID id) {
        return objects.find(id).orElseThrow(() -> new ObjectNotFoundException(id));
    }

    @Override
    public ObjectContent open(UUID id, Optional<RangeRequest> range) {
        var object = get(id);
        var resolved = range.map(request -> request.resolve(object.size()));
        var stream = read(object.blob(), resolved.map(r -> new ByteRange(r.start(), r.end())));
        return new ObjectContent(object, stream, resolved);
    }

    @Override
    public ObjectPage list(BucketName bucket, @Nullable String namePrefix, Optional<UUID> after, int limit) {
        int pageSize = Math.clamp(limit, 1, MAX_PAGE_SIZE);
        // Un élément de plus que la page : sa présence indique qu'il reste des objets.
        var found = objects.list(bucketOf(bucket).id(), namePrefix, after, pageSize + 1);
        if (found.size() <= pageSize) {
            return new ObjectPage(found, Optional.empty());
        }
        var page = found.subList(0, pageSize);
        return new ObjectPage(page, Optional.of(page.getLast().id()));
    }

    @Override
    public void delete(UUID id) {
        objects.delete(get(id));
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

    private StoredObject insertOrPurge(StoredObject object) {
        try {
            return objects.insert(object);
        } catch (RuntimeException e) {
            purgeQueue.schedule(object.blob());
            throw e;
        }
    }
}
