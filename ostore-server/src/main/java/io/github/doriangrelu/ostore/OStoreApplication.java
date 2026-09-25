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
package io.github.doriangrelu.ostore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application OStore.
 *
 * <p>Placée dans le package racine pour que le scan de composants couvre l'ensemble des
 * adaptateurs ; le câblage des cas d'usage reste explicite dans des classes de configuration.
 */
@SpringBootApplication
public class OStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(OStoreApplication.class, args);
    }
}
