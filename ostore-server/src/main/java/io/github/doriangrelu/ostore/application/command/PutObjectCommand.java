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
package io.github.doriangrelu.ostore.application.command;

import io.github.doriangrelu.ostore.domain.model.vo.BucketName;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectMetadata;
import java.io.InputStream;
import java.util.Optional;

/**
 * Dépôt d'un objet (création ou remplacement).
 *
 * @param bucket bucket cible
 * @param key clé de l'objet
 * @param contentLength taille annoncée, vérifiée à l'octet près
 * @param contentType type MIME, absent = {@code application/octet-stream}
 * @param metadata métadonnées utilisateur
 * @param content contenu, lu en flux
 */
public record PutObjectCommand(
        BucketName bucket,
        ObjectKey key,
        long contentLength,
        Optional<String> contentType,
        ObjectMetadata metadata,
        InputStream content) {}
