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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Garde-fou de la clean architecture du module serveur (ADR-0002).
 *
 * <p>Les couches étant de simples packages, seules ces règles empêchent une dépendance illégale.
 * Toute violation fait échouer le build.
 */
@AnalyzeClasses(packages = "io.github.doriangrelu.ostore", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String BASE = "io.github.doriangrelu.ostore";

    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain")
            .definedBy(BASE + ".domain..")
            .layer("Application")
            .definedBy(BASE + ".application..")
            .layer("Api")
            .definedBy(BASE + ".api..")
            .layer("Infrastructure")
            .definedBy(BASE + ".infrastructure..")
            .whereLayer("Api")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Api", "Infrastructure")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Api", "Infrastructure");

    /** Le domaine ne connaît que le JDK et les annotations de nullité. */
    @ArchTest
    static final ArchRule domainIsFrameworkFree = classes()
            .that()
            .resideInAPackage(BASE + ".domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(BASE + ".domain..", "java..", "org.jspecify..")
            .allowEmptyShould(true);

    /** Les cas d'usage ne dépendent que du domaine et de la SPI des drivers, jamais d'un framework. */
    @ArchTest
    static final ArchRule applicationIsFrameworkFree = classes()
            .that()
            .resideInAPackage(BASE + ".application..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                    BASE + ".application..", BASE + ".domain..", BASE + ".driver.spi..", "java..", "org.jspecify..")
            .allowEmptyShould(true);

    /** Le contrat HTTP est réservé aux adaptateurs REST : il ne doit pas fuiter dans le cœur. */
    @ArchTest
    static final ArchRule contractOnlyUsedByRestApi = noClasses()
            .that()
            .resideOutsideOfPackages(BASE + ".api.rest..", BASE + ".contract..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BASE + ".contract..")
            .allowEmptyShould(true);
}
