/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettings.ElideSettingsBuilder;
import com.yahoo.elide.SerdesBuilderCustomizer;
import com.yahoo.elide.core.request.route.NullRouteResolver;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.request.route.RouteResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for ElideAutoConfiguration.
 */
class ElideAutoConfigurationTest {
    private static final String TARGET_NAME_PREFIX = "scopedTarget.";
    private static final String SCOPE_REFRESH = "refresh";
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElideAutoConfiguration.class, DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class,
                    RefreshAutoConfiguration.class, ServerConfiguration.class));

    @Test
    void nonRefreshable() {
        Set<String> nonRefreshableBeans = new HashSet<>();

        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false", "elide.json-api.enabled=true",
                "elide.api-docs.enabled=true", "elide.graphql.enabled=true").run(context -> {
                    Arrays.stream(context.getBeanDefinitionNames()).forEach(beanDefinitionName -> {
                        if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                            BeanDefinition beanDefinition = beanDefinitionRegistry
                                    .getBeanDefinition(beanDefinitionName);
                            assertThat(beanDefinition.getScope()).isNotEqualTo(SCOPE_REFRESH);
                            nonRefreshableBeans.add(beanDefinitionName);
                        }
                    });
                });
        assertThat(nonRefreshableBeans).contains("refreshableElide", "graphqlController", "queryRunners",
                "apiDocsController", "apiDocsRegistrations", "jsonApiController");
    }

    enum RefreshableInput {
        GRAPHQL(new String[] { "elide.graphql.enabled=true" },
                new String[] { "refreshableElide", "graphqlController", "queryRunners" }),
        OPENAPI(new String[] { "elide.api-docs.enabled=true" },
                new String[] { "refreshableElide", "apiDocsController", "apiDocsRegistrations" }),
        JSONAPI(new String[] { "elide.json-api.enabled=true" },
                new String[] { "refreshableElide", "jsonApiController" });

        String[] propertyValues;
        String[] beanNames;

        RefreshableInput(String[] propertyValues, String[] beanNames) {
            this.propertyValues = propertyValues;
            this.beanNames = beanNames;
        }
    }

    @ParameterizedTest
    @EnumSource(RefreshableInput.class)
    void refreshable(RefreshableInput refreshableInput) {
        contextRunner.withPropertyValues(refreshableInput.propertyValues).run(context -> {

            Set<String> refreshableBeans = new HashSet<>();

            Arrays.stream(context.getBeanDefinitionNames()).forEach(beanDefinitionName -> {
                if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                    BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanDefinitionName);
                    if (SCOPE_REFRESH.equals(beanDefinition.getScope())) {
                        refreshableBeans.add(beanDefinitionName.replace(TARGET_NAME_PREFIX, ""));
                        assertThat(context.getBean(beanDefinitionName)).isNotNull();
                    }
                }
            });

            assertThat(refreshableBeans).contains(refreshableInput.beanNames);
        });
    }

    @RestController
    public static class UserGraphqlController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserGraphqlControllerConfiguration {
        @Bean
        public UserGraphqlController graphqlController() {
            return new UserGraphqlController();
        }
    }

    @RestController
    public static class UserApiDocsController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserApiDocsControllerConfiguration {
        @Bean
        public UserApiDocsController apiDocsController() {
            return new UserApiDocsController();
        }
    }

    @RestController
    public static class UserJsonApiController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserJsonApiControllerConfiguration {
        @Bean
        public UserJsonApiController jsonApiController() {
            return new UserJsonApiController();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ServerProperties.class)
    public static class ServerConfiguration {
    }

    enum OverrideControllerInput {
        GRAPHQL(new String[] { "elide.graphql.enabled=true" }, UserGraphqlControllerConfiguration.class,
                "graphqlController"),
        OPENAPI(new String[] { "elide.api-docs.enabled=true" }, UserApiDocsControllerConfiguration.class,
                "apiDocsController"),
        JSONAPI(new String[] { "elide.json-api.enabled=true" }, UserJsonApiControllerConfiguration.class,
                "jsonApiController");

        String[] propertyValues;
        Class<?> userConfiguration;
        String beanName;

        OverrideControllerInput(String[] propertyValues, Class<?> userConfiguration, String beanName) {
            this.propertyValues = propertyValues;
            this.userConfiguration = userConfiguration;
            this.beanName = beanName;
        }
    }

    @ParameterizedTest
    @EnumSource(OverrideControllerInput.class)
    void overrideController(OverrideControllerInput input) {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false").withPropertyValues(input.propertyValues)
                .withConfiguration(UserConfigurations.of(input.userConfiguration)).run(context -> {
                    if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                        BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(input.beanName);
                        assertThat(beanDefinition.getFactoryBeanName())
                                .endsWith(input.userConfiguration.getSimpleName());
                    }
                });
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserSerdesConfiguration {
        @Bean
        public SerdesBuilderCustomizer serdesBuilderCustomizer() {
            return builder -> builder.clear();
        }
    }

    @Test
    void customizeSerdes() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false")
        .withConfiguration(UserConfigurations.of(UserSerdesConfiguration.class)).run(context -> {
            ElideSettingsBuilder builder = context.getBean(ElideSettingsBuilder.class);
            ElideSettings elideSettings = builder.build();
            assertThat(elideSettings.getSerdes()).isEmpty();
        });
    }

    @Test
    void defaultSerdesConfigured() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false").run(context -> {
            ElideSettingsBuilder builder = context.getBean(ElideSettingsBuilder.class);
            ElideSettings elideSettings = builder.build();
            assertThat(elideSettings.getSerdes()).hasSize(8);
        });
    }

    @Test
    void defaultApiVersioningStrategyHeader() {
        contextRunner
                .withPropertyValues("spring.cloud.refresh.enabled=false",
                        "elide.api-versioning-strategy.header.enabled=true")
                .run(context -> {
                    RouteResolver routeResolver = context.getBean(RouteResolver.class);
                    Route route = routeResolver.resolve("", "", "", Map.of("accept-version", List.of("1")), Collections.emptyMap());
                    assertThat(route.getApiVersion()).isEqualTo("1");
                });
    }

    @Test
    void configuredApiVersioningStrategyHeader() {
        contextRunner
                .withPropertyValues("spring.cloud.refresh.enabled=false",
                        "elide.api-versioning-strategy.header.enabled=true",
                        "elide.api-versioning-strategy.header.header-name=ApiVersion")
                .run(context -> {
                    RouteResolver routeResolver = context.getBean(RouteResolver.class);
                    Route route = routeResolver.resolve("", "", "", Map.of("apiversion", List.of("1")), Collections.emptyMap());
                    assertThat(route.getApiVersion()).isEqualTo("1");
                });
    }

    @Test
    void defaultApiVersioningStrategyParameter() {
        contextRunner
                .withPropertyValues("spring.cloud.refresh.enabled=false",
                        "elide.api-versioning-strategy.parameter.enabled=true")
                .run(context -> {
                    RouteResolver routeResolver = context.getBean(RouteResolver.class);
                    Route route = routeResolver.resolve("", "", "", Collections.emptyMap(), Map.of("v", List.of("1")));
                    assertThat(route.getApiVersion()).isEqualTo("1");
                });
    }

    @Test
    void defaultApiVersioningMediaTypeProfile() {
        contextRunner
                .withPropertyValues("spring.cloud.refresh.enabled=false",
                        "elide.api-versioning-strategy.media-type-profile.enabled=true",
                        "elide.base-url=https://example.org")
                .run(context -> {
                    String accept = "application/vnd.api+json; profile=\"https://example.org/api/v1 https://example.org/profile\"";
                    RouteResolver routeResolver = context.getBean(RouteResolver.class);
                    Route route = routeResolver.resolve("", "", "", Map.of("accept", List.of(accept)), Collections.emptyMap());
                    assertThat(route.getApiVersion()).isEqualTo("1");
                });
    }

    @Test
    void noApiVersioning() {
        contextRunner
                .withPropertyValues("spring.cloud.refresh.enabled=false",
                        "elide.api-versioning-strategy.path.enabled=false")
                .run(context -> {
                    RouteResolver routeResolver = context.getBean(RouteResolver.class);
                    assertThat(routeResolver).isInstanceOf(NullRouteResolver.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserGroupedOpenApiConfiguration {
        @Bean
        public GroupedOpenApi groupedOpenApi() {
            return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/v1").build();
        }
    }

    @Test
    void groupedOpenApi() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false")
        .withConfiguration(UserConfigurations.of(UserGroupedOpenApiConfiguration.class)).run(context -> {
            GroupedOpenApi groupedOpenApi = context.getBean(GroupedOpenApi.class);
            assertThat(groupedOpenApi.getOpenApiCustomizers()).hasSize(1);
        });
    }
}
