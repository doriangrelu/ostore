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
package io.github.doriangrelu.ostore.contract.constant;

/** Chemins de l'API REST, partagés entre le serveur et les clients. */
public final class ApiPaths {

    /** Racine de l'API REST, version 1. */
    public static final String API_V1 = "/api/v1";

    /** Collection des buckets. */
    public static final String BUCKETS = API_V1 + "/buckets";

    /** Objets d'un bucket : dépôt et liste ({@code {bucket}} = nom du bucket). */
    public static final String BUCKET_OBJECTS = BUCKETS + "/{bucket}/objects";

    /** Un objet, désigné par son identifiant ({@code {id}}). */
    public static final String OBJECT = API_V1 + "/objects/{id}";

    /** Contenu binaire d'un objet. */
    public static final String OBJECT_CONTENT = OBJECT + "/content";

    /** Copie d'un objet. */
    public static final String OBJECT_COPY = OBJECT + "/copy";

    /** Collection des transactions. */
    public static final String TRANSACTIONS = API_V1 + "/transactions";

    /** Une transaction, désignée par son identifiant ({@code {id}}). */
    public static final String TRANSACTION = TRANSACTIONS + "/{id}";

    /** Objets d'une transaction. */
    public static final String TRANSACTION_OBJECTS = TRANSACTION + "/objects";

    /** Validation d'une transaction. */
    public static final String TRANSACTION_COMMIT = TRANSACTION + "/commit";

    /** Annulation d'une transaction. */
    public static final String TRANSACTION_ROLLBACK = TRANSACTION + "/rollback";

    /** Prolongation d'une transaction. */
    public static final String TRANSACTION_EXTEND = TRANSACTION + "/extend";

    private ApiPaths() {}
}
