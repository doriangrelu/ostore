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

    private ApiPaths() {}
}
