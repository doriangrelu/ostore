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

/** En-têtes HTTP propres à OStore. */
public final class OStoreHeaders {

    /**
     * Métadonnée utilisateur, répétable : {@code X-OStore-Meta: nom=valeur}. La valeur est percent-encodée en
     * UTF-8 (RFC 3986), ce qui autorise tout caractère, y compris la virgule. Envoyé au dépôt, renvoyé à la
     * lecture du contenu.
     */
    public static final String META = "X-OStore-Meta";

    /** Identifiant de ressource de l'objet, renvoyé à la lecture du contenu. */
    public static final String RESOURCE_ID = "X-OStore-Resource-Id";

    private OStoreHeaders() {}
}
