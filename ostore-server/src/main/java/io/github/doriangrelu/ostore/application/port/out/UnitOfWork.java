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
package io.github.doriangrelu.ostore.application.port.out;

import java.util.function.Supplier;

/**
 * Transaction de base de données englobant plusieurs appels de ports : tout est validé ou rien ne l'est. Expose
 * la transactionnalité au cœur applicatif sans dépendance à un framework (ADR-0016).
 */
public interface UnitOfWork {

    /** Exécute le travail dans une transaction (ou dans la transaction courante s'il y en a une). */
    <T> T execute(Supplier<T> work);
}
