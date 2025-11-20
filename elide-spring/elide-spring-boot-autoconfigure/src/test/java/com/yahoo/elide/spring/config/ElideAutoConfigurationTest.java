/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yahoo.elide.ElideErrorResponse;
import com.yahoo.elide.ElideErrors;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettings.ElideSettingsBuilder;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.SerdesBuilderCustomizer;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary.EntityDictionaryBuilder;
import com.yahoo.elide.core.exceptions.ErrorContext;
import com.yahoo.elide.core.exceptions.ExceptionMapper;
import com.yahoo.elide.core.exceptions.ExceptionMapperRegistration;
import com.yahoo.elide.core.request.route.NullRouteResolver;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.request.route.RouteResolver;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.graphql.GraphQLErrorContext;
import com.yahoo.elide.graphql.GraphQLExceptionHandler;
import com.yahoo.elide.graphql.GraphQLSettings;
import com.yahoo.elide.graphql.models.GraphQLErrors;
import com.yahoo.elide.jsonapi.JsonApiErrorContext;
import com.yahoo.elide.jsonapi.JsonApiExceptionHandler;
import com.yahoo.elide.jsonapi.JsonApiSettings;
import com.yahoo.elide.jsonapi.models.JsonApiError;
import com.yahoo.elide.jsonapi.models.JsonApiErrors;
import com.yahoo.elide.modelconfig.DynamicConfiguration;
import com.yahoo.elide.spring.datastore.config.DataStoreBuilder;

import example.AppConfiguration;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import graphql.GraphQLError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
            .withConfiguration(UserConfigurations.of(AppConfiguration.class))
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

    public static class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException, ElideErrors> {
        @Override
        public ElideErrorResponse<ElideErrors> toErrorResponse(ConstraintViolationException exception,
                ErrorContext errorContext) {
            return ElideErrorResponse.badRequest(ElideErrors.builder()
                    .error(error -> error.message(exception.getMessage()).attribute("constraint",
                            exception.getConstraintName()))
                    .build());
        }
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserExceptionHandlerConfiguration {
        @Bean
        public ExceptionMapperRegistration exceptionMapperRegistration() {
            return ExceptionMapperRegistration.builder().exceptionMapper(new ConstraintViolationExceptionMapper()).build();
        }
    }

    @Test
    void jsonApiExceptionHandler() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false", "elide.jsonapi.enabled=true")
                .withConfiguration(UserConfigurations.of(UserExceptionHandlerConfiguration.class)).run(context -> {
                    ElideSettings elideSettings = context.getBean(RefreshableElide.class).getElide().getElideSettings();
                    JsonApiSettings jsonApiSettings = elideSettings.getSettings(JsonApiSettings.class);
                    JsonApiExceptionHandler exceptionHandler = jsonApiSettings.getJsonApiExceptionHandler();
                    ConstraintViolationException exception = new ConstraintViolationException("could not execute statement",
                            null, "UC_PERSON");
                    ElideResponse<?> response = exceptionHandler.handleException(exception,
                            JsonApiErrorContext.builder().mapper(jsonApiSettings.getJsonApiMapper()).build());
                    JsonApiErrors errors = response.getBody(JsonApiErrors.class);
                    JsonApiError error = errors.getErrors().get(0);
                    assertThat(error.getDetail()).isEqualTo(exception.getMessage());
                    Map<String, Object> meta = error.getMeta();
                    assertThat(meta).containsEntry("constraint", exception.getConstraintName());
                });
    }

    @Test
    void graphqlExceptionHandler() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false", "elide.graphql.enabled=true")
                .withConfiguration(UserConfigurations.of(UserExceptionHandlerConfiguration.class)).run(context -> {
                    ElideSettings elideSettings = context.getBean(RefreshableElide.class).getElide().getElideSettings();
                    GraphQLSettings graphqlSettings = elideSettings.getSettings(GraphQLSettings.class);
                    GraphQLExceptionHandler exceptionHandler = graphqlSettings.getGraphqlExceptionHandler();
                    ConstraintViolationException exception = new ConstraintViolationException("could not execute statement",
                            null, "UC_PERSON");
                    ElideResponse<?> response = exceptionHandler.handleException(exception,
                            GraphQLErrorContext.builder().build());
                    GraphQLErrors errors = response.getBody(GraphQLErrors.class);
                    GraphQLError error = errors.getErrors().get(0);
                    assertThat(error.getMessage()).isEqualTo(exception.getMessage());
                    Map<String, Object> meta = error.getExtensions();
                    assertThat(meta).containsEntry("constraint", exception.getConstraintName());
                });
    }

    @Test
    void asyncEnabledJsonApiEnabledGraphqlNotEnabled() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false", "elide.async.enabled=true", "elide.graphql.enabled=false", "elide.json-api.enabled=true")
                .withConfiguration(AutoConfigurations.of(ElideAsyncConfiguration.class))
                .withConfiguration(UserConfigurations.of(UserExceptionHandlerConfiguration.class)).run(context -> {
                    ElideSettings elideSettings = context.getBean(RefreshableElide.class).getElide().getElideSettings();
                    GraphQLSettings graphqlSettings = elideSettings.getSettings(GraphQLSettings.class);
                    assertThat(graphqlSettings).isNull();
                    JsonApiSettings jsonApiSettings = elideSettings.getSettings(JsonApiSettings.class);
                    assertThat(jsonApiSettings).isNotNull();
                    AsyncExecutorService asyncExecutorService = context.getBean(AsyncExecutorService.class);
                    assertThat(asyncExecutorService).isNotNull();
                });
    }

    @Test
    void aggregationEnabled() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false", "elide.json-api.enabled=true", "elide.aggregation-store.enabled=true")
                .run(context -> {
                    DataStoreBuilder dataStoreBuilder = context.getBean(DataStoreBuilder.class);
                    List<DataStore> result = new ArrayList<>();
                    dataStoreBuilder.dataStores(datastores -> result.addAll(datastores));
                    assertThat(result).isNotEmpty();
                    assertThat(result).hasAtLeastOneElementOfType(MetaDataStore.class);
                    assertThat(result).hasAtLeastOneElementOfType(AggregationDataStore.class);
                });
    }

    @Test
    void aggregationEnabledDynamicConfigurationEnabled() {
        contextRunner
                .withPropertyValues("spring.cloud.refresh.enabled=false", "elide.json-api.enabled=true",
                        "elide.aggregation-store.enabled=true", "elide.aggregation-store.dynamic-config.enabled=true",
                        "elide.aggregation-store.dynamic-config.path=configs")
                .run(context -> {
                    EntityDictionaryBuilder entityDictionaryBuilder = context.getBean(EntityDictionaryBuilder.class);
                    Map<String, UserCheck> result = new LinkedHashMap<>();
                    entityDictionaryBuilder.roleChecks(roleChecks -> result.putAll(roleChecks));
                    assertThat(result).isNotEmpty();
                    // Roles in configs/models/security.hjson
                    assertThat(result).containsKey("admin.user");
                    assertThat(result).containsKey("guest.user");
                    DynamicConfiguration dynamicConfiguration = context.getBean(DynamicConfiguration.class);
                    assertThat(dynamicConfiguration).isNotNull();
                });
    }
}
