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

/**
 * Copie d'un objet (contenu, type et métadonnées) vers une autre clé, éventuellement dans un autre bucket.
 *
 * @param sourceBucket bucket source
 * @param sourceKey clé source
 * @param targetBucket bucket cible
 * @param targetKey clé cible
 */
public record CopyObjectCommand(
        BucketName sourceBucket, ObjectKey sourceKey, BucketName targetBucket, ObjectKey targetKey) {}
