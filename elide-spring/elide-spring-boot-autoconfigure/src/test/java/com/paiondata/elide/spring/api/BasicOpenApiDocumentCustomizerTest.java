/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.api;


import static org.assertj.core.api.Assertions.assertThat;

import com.paiondata.elide.swagger.OpenApiDocument;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * BasicOpenApiDocumentCustomizerTest.
 */
class BasicOpenApiDocumentCustomizerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Configuration
    @OpenAPIDefinition(info = @Info(title = "My Title"))
    public static class UserDefinitionOpenApiConfiguration {
        @Bean
        public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
            return new BasicOpenApiDocumentCustomizer();
        }
    }

    @Configuration
    @OpenAPIDefinition(info = @Info(description = "My Description"))
    public static class UserDefinitionNoTitleOpenApiConfiguration {
        @Bean
        public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
            return new BasicOpenApiDocumentCustomizer();
        }
    }

    @Configuration
    public static class UserNoDefinitionOpenApiConfiguration {
        @Bean
        public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
            return new BasicOpenApiDocumentCustomizer();
        }
    }

    @Configuration
    @OpenAPIDefinition(info = @Info(title = "My Title"), security = @SecurityRequirement(name = "bearerAuth"))
    @SecurityScheme(
            name = "bearerAuth",
            type = SecuritySchemeType.HTTP,
            bearerFormat = "JWT",
            scheme = "bearer"
        )
    public static class UserSecurityOpenApiConfiguration {
        @Bean
        public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
            return new BasicOpenApiDocumentCustomizer();
        }
    }

    @Test
    void shouldUseOpenApiDefinition() {
        contextRunner.withUserConfiguration(UserDefinitionOpenApiConfiguration.class).run(context -> {
            OpenAPI openApi = new OpenAPI();
            context.getBean(OpenApiDocumentCustomizer.class).customize(openApi);
            assertThat(openApi.getInfo().getTitle()).isEqualTo("My Title");
        });
    }

    @Test
    void shouldUseDefaultTitleNoDefinition() {
        contextRunner.withUserConfiguration(UserNoDefinitionOpenApiConfiguration.class).run(context -> {
            OpenAPI openApi = new OpenAPI();
            context.getBean(OpenApiDocumentCustomizer.class).customize(openApi);
            assertThat(openApi.getInfo().getTitle()).isEqualTo(OpenApiDocument.DEFAULT_TITLE);
        });
    }

    @Test
    void shouldUseDefaultTitleDefinitionNoTitle() {
        contextRunner.withUserConfiguration(UserDefinitionNoTitleOpenApiConfiguration.class).run(context -> {
            OpenAPI openApi = new OpenAPI();
            context.getBean(OpenApiDocumentCustomizer.class).customize(openApi);
            assertThat(openApi.getInfo().getTitle()).isEqualTo(OpenApiDocument.DEFAULT_TITLE);
        });
    }

    @Test
    void shouldUseSecurityRequirements() {
        contextRunner.withUserConfiguration(UserSecurityOpenApiConfiguration.class).run(context -> {
            OpenAPI openApi = new OpenAPI();
            context.getBean(OpenApiDocumentCustomizer.class).customize(openApi);
            assertThat(openApi.getSecurity().get(0)).containsKey("bearerAuth");
            io.swagger.v3.oas.models.security.SecurityScheme securityScheme =
                    openApi.getComponents().getSecuritySchemes().get("bearerAuth");
            assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
            assertThat(securityScheme.getScheme()).isEqualTo("bearer");
        });
    }

    @Test
    void shouldSortTags() {
        contextRunner.withUserConfiguration(UserDefinitionOpenApiConfiguration.class).run(context -> {
            OpenAPI openApi = new OpenAPI();
            openApi.addTagsItem(new Tag().name("a-test"));
            openApi.addTagsItem(new Tag().name("z-test"));
            openApi.addTagsItem(new Tag().name("b-test"));
            openApi.addTagsItem(new Tag().name("1-test"));
            context.getBean(OpenApiDocumentCustomizer.class).customize(openApi);
            assertThat(openApi.getTags()).extracting("name").containsExactly("1-test", "a-test", "b-test", "z-test");
        });
    }
}
