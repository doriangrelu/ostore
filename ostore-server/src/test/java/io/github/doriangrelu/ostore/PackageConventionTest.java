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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.doriangrelu.ostore.domain.exception.DomainException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Packages typés (directive utilisateur, docs/conventions/code.md) : chaque type de classe vit dans son
 * package dédié, jamais mélangé avec d'autres types.
 */
@AnalyzeClasses(packages = "io.github.doriangrelu.ostore", importOptions = ImportOption.DoNotIncludeTests.class)
class PackageConventionTest {

    @ArchTest
    static final ArchRule configurations = classes()
            .that()
            .areAnnotatedWith(Configuration.class)
            .should()
            .resideInAPackage("..infrastructure.config..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule configurationProperties = classes()
            .that()
            .areAnnotatedWith(ConfigurationProperties.class)
            .should()
            .resideInAPackage("..infrastructure.properties..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage("..api.*.controller..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule exceptionHandlers = classes()
            .that()
            .areAnnotatedWith(RestControllerAdvice.class)
            .should()
            .resideInAPackage("..api.*.handler..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistenceEntities = classes()
            .that()
            .areAnnotatedWith(Table.class)
            .should()
            .resideInAPackage("..infrastructure.persistence.entity..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule springDataRepositories = classes()
            .that()
            .areInterfaces()
            .and()
            .areAssignableTo(Repository.class)
            .should()
            .resideInAPackage("..infrastructure.persistence.repository..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule converters = classes()
            .that()
            .areAssignableTo(Converter.class)
            .should()
            .resideInAPackage("..infrastructure.persistence.converter..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainExceptions = classes()
            .that()
            .areAssignableTo(DomainException.class)
            .should()
            .resideInAPackage("..domain.exception..")
            .allowEmptyShould(true);

    /** Un service n'est jamais un record : ses accesseurs publics exposeraient les ports de sortie. */
    @ArchTest
    static final ArchRule servicesAreNotRecords = noClasses()
            .that()
            .resideInAPackage("..application.service..")
            .should()
            .beAssignableTo(Record.class)
            .allowEmptyShould(true);

    /** Les dépendances d'un service restent encapsulées. */
    @ArchTest
    static final ArchRule serviceFieldsArePrivateFinal = fields().that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..application.service..")
            .should()
            .bePrivate()
            .andShould()
            .beFinal()
            .allowEmptyShould(true);
}
