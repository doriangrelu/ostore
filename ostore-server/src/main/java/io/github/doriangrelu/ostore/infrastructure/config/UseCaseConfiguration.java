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
package io.github.doriangrelu.ostore.infrastructure.config;

import io.github.doriangrelu.ostore.application.BucketService;
import io.github.doriangrelu.ostore.application.port.in.BucketUseCases;
import io.github.doriangrelu.ostore.application.port.out.BucketRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage explicite des cas d'usage : le domaine et l'application restent sans annotation Spring
 * (ADR-0002).
 */
@Configuration(proxyBeanMethods = false)
class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    BucketUseCases bucketUseCases(
            BucketRepository buckets, Clock clock, @Value("${ostore.storage.default-driver}") String defaultDriverId) {
        return new BucketService(buckets, clock, defaultDriverId);
    }
}
