/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for ApiDocsControllerProperties.
 */
class ApiDocsControllerPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @EnableConfigurationProperties(ElideConfigProperties.class)
    public static class UserPropertiesConfiguration {
    }


    @Test
    void version() {
        contextRunner.withPropertyValues("elide.api-docs.version=openapi_3_1")
                .withUserConfiguration(UserPropertiesConfiguration.class).run(context -> {
                    ElideConfigProperties properties = context.getBean(ElideConfigProperties.class);
                    assertThat(properties.getApiDocs().getVersion()).isEqualTo(ApiDocsControllerProperties.Version.OPENAPI_3_1);
                });
    }
}
