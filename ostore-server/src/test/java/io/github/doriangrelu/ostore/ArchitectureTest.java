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

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * Directive ADR-0003 : un contrôleur REST implémente une interface du contrat et ne déclare
     * <b>rien d'autre</b> (ni mapping, ni validation, ni documentation), tout vient du contrat.
     */
    @ArchTest
    static final ArchRule restControllersOnlyImplementTheContract = classes()
            .that()
            .resideInAPackage(BASE + ".api.rest..")
            .and()
            .areAnnotatedWith(RestController.class)
            .should(implementAContractInterface())
            .andShould(declareNoWebAnnotationOnMethodsOrParameters())
            .allowEmptyShould(true);

    private static ArchCondition<JavaClass> implementAContractInterface() {
        return new ArchCondition<>("implement an interface of the contract module") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                boolean implementsContract = controller.getAllRawInterfaces().stream()
                        .anyMatch(type -> type.getPackageName().startsWith(BASE + ".contract"));
                if (!implementsContract) {
                    events.add(SimpleConditionEvent.violated(
                            controller, controller.getName() + " does not implement a contract interface"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> declareNoWebAnnotationOnMethodsOrParameters() {
        return new ArchCondition<>("declare no web, validation or OpenAPI annotation on methods or parameters") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                for (JavaMethod method : controller.getMethods()) {
                    var annotations = java.util.stream.Stream.concat(
                            method.getAnnotations().stream(),
                            method.getParameters().stream().flatMap(p -> p.getAnnotations().stream()));
                    annotations
                            .map(annotation -> annotation.getRawType().getPackageName())
                            .filter(ArchitectureTest::isContractOnlyAnnotation)
                            .findFirst()
                            .ifPresent(pkg -> events.add(SimpleConditionEvent.violated(
                                    method, method.getFullName() + " redeclares an annotation from " + pkg)));
                }
            }
        };
    }

    private static boolean isContractOnlyAnnotation(String annotationPackage) {
        return annotationPackage.startsWith("org.springframework.web.bind.annotation")
                || annotationPackage.startsWith("jakarta.validation")
                || annotationPackage.startsWith("io.swagger.v3.oas.annotations");
    }
}
