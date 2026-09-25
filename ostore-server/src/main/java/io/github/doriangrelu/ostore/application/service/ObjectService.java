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
import io.github.doriangrelu.ostore.application.command.PutObjectCommand;
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
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
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

/**
 * Implémentation des cas d'usage des objets.
 *
 * <p>Ordre des écritures (ADR-0006) : le blob est écrit <b>d'abord</b>, sous un chemin neuf, puis les
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
    public StoredObject put(PutObjectCommand command) {
        var bucket = bucketOf(command.bucket());
        var now = clock.instant();
        var location = newBlobLocation(bucket, now);
        var written = writeBlob(location, command.content(), command.contentLength());
        var object = StoredObject.create(
                bucket,
                command.key(),
                location,
                written.count(),
                written.md5Hex(),
                command.contentType().orElse(StoredObject.DEFAULT_CONTENT_TYPE),
                command.metadata(),
                now);
        return persistOrPurge(object);
    }

    @Override
    public StoredObject copy(CopyObjectCommand command) {
        var source = find(command.sourceBucket(), command.sourceKey());
        var target = bucketOf(command.targetBucket());
        var now = clock.instant();
        var location = newBlobLocation(target, now);
        MeteredInputStream written;
        try (var content = read(source.blob(), Optional.empty())) {
            written = writeBlob(location, content, source.size());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var copy = StoredObject.create(
                target,
                command.targetKey(),
                location,
                written.count(),
                written.md5Hex(),
                source.contentType(),
                source.metadata(),
                now);
        return persistOrPurge(copy);
    }

    @Override
    public StoredObject get(BucketName bucket, ObjectKey key) {
        return find(bucket, key);
    }

    @Override
    public ObjectContent open(BucketName bucket, ObjectKey key, Optional<RangeRequest> range) {
        var object = find(bucket, key);
        var resolved = range.map(request -> request.resolve(object.size()));
        var stream = read(object.blob(), resolved.map(r -> new ByteRange(r.start(), r.end())));
        return new ObjectContent(object, stream, resolved);
    }

    @Override
    public ObjectPage list(BucketName bucket, String prefix, Optional<ObjectKey> after, int maxKeys) {
        int pageSize = Math.clamp(maxKeys, 1, MAX_PAGE_SIZE);
        // Un élément de plus que la page : sa présence indique qu'il reste des objets.
        var found = objects.list(bucketOf(bucket).id(), prefix, after, pageSize + 1);
        if (found.size() <= pageSize) {
            return new ObjectPage(found, Optional.empty());
        }
        var page = found.subList(0, pageSize);
        return new ObjectPage(page, Optional.of(page.getLast().key()));
    }

    @Override
    public void delete(BucketName bucket, ObjectKey key) {
        objects.delete(find(bucket, key));
    }

    private Bucket bucketOf(BucketName name) {
        return buckets.findByName(name).orElseThrow(() -> new BucketNotFoundException(name));
    }

    private StoredObject find(BucketName bucketName, ObjectKey key) {
        var bucket = bucketOf(bucketName);
        return objects.find(bucket.id(), key).orElseThrow(() -> new ObjectNotFoundException(bucketName, key));
    }

    private BlobLocation newBlobLocation(Bucket bucket, Instant now) {
        var path = drivers.layout(bucket.driverId()).pathOf(Identifiers.newId(now), now);
        return new BlobLocation(bucket.driverId(), path.value());
    }

    private InputStream read(BlobLocation location, Optional<ByteRange> range) {
        return drivers.driver(location.driverId()).read(new BlobPath(location.path()), range);
    }

    /** Écrit le blob et vérifie sa taille ; le flux mesuré renvoyé donne la taille et le MD5 reçus. */
    private MeteredInputStream writeBlob(BlobLocation location, InputStream content, long expectedSize) {
        var metered = new MeteredInputStream(content);
        try {
            drivers.driver(location.driverId()).write(new BlobPath(location.path()), metered, expectedSize);
            metered.requireExactly(expectedSize);
            return metered;
        } catch (RuntimeException e) {
            purgeQueue.schedule(location);
            throw e;
        }
    }

    private StoredObject persistOrPurge(StoredObject object) {
        try {
            return objects.putActive(object);
        } catch (RuntimeException e) {
            purgeQueue.schedule(object.blob());
            throw e;
        }
    }
}
