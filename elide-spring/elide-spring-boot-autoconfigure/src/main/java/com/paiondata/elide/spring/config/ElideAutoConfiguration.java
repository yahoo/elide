/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideMapper;
import com.paiondata.elide.ElideSettings.ElideSettingsBuilder;
import com.paiondata.elide.ElideSettingsBuilderCustomizer;
import com.paiondata.elide.ElideSettingsBuilderCustomizers;
import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.Serdes;
import com.paiondata.elide.Serdes.SerdesBuilder;
import com.paiondata.elide.SerdesBuilderCustomizer;
import com.paiondata.elide.Settings.SettingsBuilder;
import com.paiondata.elide.async.AsyncSettings.AsyncSettingsBuilder;
import com.paiondata.elide.async.AsyncSettingsBuilderCustomizer;
import com.paiondata.elide.async.AsyncSettingsBuilderCustomizers;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.audit.Slf4jLogger;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.EntityDictionary.EntityDictionaryBuilder;
import com.paiondata.elide.core.dictionary.EntityDictionaryBuilderCustomizer;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.exceptions.BasicExceptionMappers;
import com.paiondata.elide.core.exceptions.ExceptionLogger;
import com.paiondata.elide.core.exceptions.ExceptionMapper;
import com.paiondata.elide.core.exceptions.ExceptionMapperRegistration;
import com.paiondata.elide.core.exceptions.ExceptionMappers;
import com.paiondata.elide.core.exceptions.ExceptionMappers.ExceptionMappersBuilder;
import com.paiondata.elide.core.exceptions.ExceptionMappersBuilderCustomizer;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.request.route.ApiVersionValidator;
import com.paiondata.elide.core.request.route.BasicApiVersionValidator;
import com.paiondata.elide.core.request.route.DelegatingRouteResolver;
import com.paiondata.elide.core.request.route.HeaderRouteResolver;
import com.paiondata.elide.core.request.route.MediaTypeProfileRouteResolver;
import com.paiondata.elide.core.request.route.NullRouteResolver;
import com.paiondata.elide.core.request.route.ParameterRouteResolver;
import com.paiondata.elide.core.request.route.PathRouteResolver;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.UserCheck;
import com.paiondata.elide.core.security.checks.prefab.Role;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import com.paiondata.elide.datastores.aggregation.AggregationDataStore;
import com.paiondata.elide.datastores.aggregation.DefaultQueryValidator;
import com.paiondata.elide.datastores.aggregation.QueryEngine;
import com.paiondata.elide.datastores.aggregation.cache.Cache;
import com.paiondata.elide.datastores.aggregation.cache.CaffeineCache;
import com.paiondata.elide.datastores.aggregation.core.QueryLogger;
import com.paiondata.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.DataSourceConfiguration;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.query.AggregateBeforeJoinOptimizer;
import com.paiondata.elide.datastores.aggregation.validator.TemplateConfigValidator;
import com.paiondata.elide.datastores.jpa.JpaDataStore;
import com.paiondata.elide.graphql.DefaultGraphQLErrorMapper;
import com.paiondata.elide.graphql.DefaultGraphQLExceptionHandler;
import com.paiondata.elide.graphql.GraphQLErrorMapper;
import com.paiondata.elide.graphql.GraphQLExceptionHandler;
import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;
import com.paiondata.elide.graphql.GraphQLSettingsBuilderCustomizer;
import com.paiondata.elide.graphql.GraphQLSettingsBuilderCustomizers;
import com.paiondata.elide.graphql.QueryRunners;
import com.paiondata.elide.jsonapi.DefaultJsonApiErrorMapper;
import com.paiondata.elide.jsonapi.DefaultJsonApiExceptionHandler;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.jsonapi.JsonApiErrorMapper;
import com.paiondata.elide.jsonapi.JsonApiExceptionHandler;
import com.paiondata.elide.jsonapi.JsonApiMapper;
import com.paiondata.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;
import com.paiondata.elide.jsonapi.JsonApiSettingsBuilderCustomizer;
import com.paiondata.elide.jsonapi.JsonApiSettingsBuilderCustomizers;
import com.paiondata.elide.jsonapi.links.DefaultJsonApiLinks;
import com.paiondata.elide.modelconfig.DBPasswordExtractor;
import com.paiondata.elide.modelconfig.DynamicConfiguration;
import com.paiondata.elide.modelconfig.store.ConfigDataStore;
import com.paiondata.elide.modelconfig.store.models.ConfigChecks;
import com.paiondata.elide.modelconfig.validator.DynamicConfigValidator;
import com.paiondata.elide.spring.api.BasicOpenApiDocumentCustomizer;
import com.paiondata.elide.spring.api.DefaultElideGroupedOpenApiCustomizer;
import com.paiondata.elide.spring.api.DefaultElideOpenApiCustomizer;
import com.paiondata.elide.spring.api.ElideGroupedOpenApiCustomizer;
import com.paiondata.elide.spring.api.ElideOpenApiCustomizer;
import com.paiondata.elide.spring.api.OpenApiDocumentCustomizer;
import com.paiondata.elide.spring.controllers.ApiDocsController;
import com.paiondata.elide.spring.controllers.ApiDocsController.ApiDocsRegistration;
import com.paiondata.elide.spring.controllers.ExportController;
import com.paiondata.elide.spring.controllers.GraphqlController;
import com.paiondata.elide.spring.controllers.JsonApiController;
import com.paiondata.elide.spring.datastore.config.DataStoreBuilder;
import com.paiondata.elide.spring.datastore.config.DataStoreBuilderCustomizer;
import com.paiondata.elide.spring.jackson.ObjectMapperBuilder;
import com.paiondata.elide.spring.orm.jpa.config.EnableJpaDataStore;
import com.paiondata.elide.spring.orm.jpa.config.EnableJpaDataStores;
import com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistration;
import com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistrations;
import com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistrationsBuilder;
import com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistrationsBuilderCustomizer;
import com.paiondata.elide.swagger.OpenApiBuilder;
import com.paiondata.elide.utils.HeaderProcessor;
import com.paiondata.elide.utils.Headers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.function.SingletonSupplier;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

/**
 * Auto Configuration For Elide Services.  Override any of the beans (by defining your own) to change
 * the default behavior.
 */
@Configuration
@AutoConfigureAfter(TransactionAutoConfiguration.class)
@EnableConfigurationProperties(ElideConfigProperties.class)
@Slf4j
public class ElideAutoConfiguration {
    /**
     * Creates the {@link AuditLogger}.
     *
     * @return the AuditLogger
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditLogger auditLogger() {
        return new Slf4jLogger();
    }

    /**
     * Creates the {@link ElideSettingsBuilder}.
     * <p>
     * Defining a {@link ElideSettingsBuilderCustomizer} will allow customization of the default builder.
     *
     * @param settings the settings
     * @param entityDictionary the entity dictionary
     * @param dataStore the data store
     * @param headerProcessor the header processor
     * @param elideMapper the elide mapper
     * @param settingsProvider the settings
     * @param customizerProvider the customizer
     * @return the ElideSettingsBuilder
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public ElideSettingsBuilder elideSettingsBuilder(ElideConfigProperties settings, EntityDictionary entityDictionary,
            DataStore dataStore, HeaderProcessor headerProcessor, ElideMapper elideMapper, AuditLogger auditLogger,
            SerdesBuilder serdesBuilder, ObjectProvider<SettingsBuilder> settingsProvider,
            ObjectProvider<ElideSettingsBuilderCustomizer> customizerProvider) {
        return ElideSettingsBuilderCustomizers.buildElideSettingsBuilder(builder -> {
            builder.dataStore(dataStore).entityDictionary(entityDictionary).objectMapper(elideMapper.getObjectMapper())
                    .maxPageSize(settings.getMaxPageSize())
                    .defaultPageSize(settings.getDefaultPageSize()).auditLogger(auditLogger)
                    .baseUrl(settings.getBaseUrl())
                    .serdes(serdes -> serdes.entries(entries -> {
                        entries.clear();
                        serdesBuilder.build().entrySet().stream().forEach(entry -> {
                            entries.put(entry.getKey(), entry.getValue());
                        });
                    }))
                    .headerProcessor(headerProcessor);
            if (settings.isVerboseErrors()) {
                builder.verboseErrors(true);
            }
            settingsProvider.orderedStream().forEach(builder::settings);
            customizerProvider.orderedStream().forEach(customizer -> customizer.customize(builder));
        });
    }

    /**
     * Creates the {@link SerdesBuilder}.
     * <p>
     * Defining a {@link SerdesBuilderCustomizer} will allow customization of the default builder.
     *
     * @param customizerProvider the customizer
     * @return the SerdesBuilder
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public SerdesBuilder serdesBuilder(ObjectProvider<SerdesBuilderCustomizer> customizerProvider) {
        SerdesBuilder builder = Serdes.builder()
                .withDefaults()
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
        customizerProvider.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

    /**
     * Creates the entity dictionary for Elide which contains static metadata about Elide models.
     * Override to load check classes or life cycle hooks.
     * @param injector Injector to inject Elide models.
     * @param scanner the class scanner
     * @param optionalDynamicConfig An instance of DynamicConfiguration.
     * @param settings Elide configuration settings.
     * @param entitiesToExclude set of Entities to exclude from binding.
     * @param customizerProvider the customizers
     * @return the EntityDictionaryBuilder
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public EntityDictionaryBuilder entityDictionaryBuilder(Injector injector, ClassScanner scanner,
            Optional<DynamicConfiguration> optionalDynamicConfig, ElideConfigProperties settings,
            @Qualifier("entitiesToExclude") Set<Type<?>> entitiesToExclude,
            ObjectProvider<EntityDictionaryBuilderCustomizer> customizerProvider) {
        EntityDictionaryBuilder builder = EntityDictionary.builder();

        Map<String, Class<? extends Check>> checks = new HashMap<>();
        if (settings.getAggregationStore().getDynamicConfig().getConfigApi().isEnabled()) {
            checks.put(ConfigChecks.CAN_CREATE_CONFIG, ConfigChecks.CanNotCreate.class);
            checks.put(ConfigChecks.CAN_READ_CONFIG, ConfigChecks.CanNotRead.class);
            checks.put(ConfigChecks.CAN_DELETE_CONFIG, ConfigChecks.CanNotDelete.class);
            checks.put(ConfigChecks.CAN_UPDATE_CONFIG, ConfigChecks.CanNotUpdate.class);
        }

        Map<String, UserCheck> roleChecks = new HashMap<>();
        if (isAggregationStoreEnabled(settings) && isDynamicConfigEnabled(settings)) {
            optionalDynamicConfig.ifPresent(dynamicConfig -> dynamicConfig.getRoles()
                    .forEach(role -> roleChecks.put(role, new Role.RoleMemberCheck(role))));
        }

        builder.checks(checks).roleChecks(roleChecks).injector(injector).serdeLookup(CoerceUtil::lookup)
                .entitiesToExclude(entitiesToExclude).scanner(scanner);

        customizerProvider.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

    /**
     * Creates the validator to determine if a string represents a valid api version.
     *
     * @return the validator
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiVersionValidator apiVersionValidator() {
        return new BasicApiVersionValidator();
    }

    /**
     * Creates the route resolver to determine the api version of the route.
     *
     * @param refreshableElide Singleton elide instance.
     * @param settings Config Settings.
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RouteResolver routeResolver(RefreshableElide refreshableElide, ElideConfigProperties settings,
            ApiVersionValidator apiVersionValidator) {
        Set<String> apiVersions = refreshableElide.getElide().getElideSettings().getEntityDictionary().getApiVersions();
        if (apiVersions.size() == 1 && apiVersions.contains(EntityDictionary.NO_VERSION)) {
            return new NullRouteResolver();
        } else {
            List<RouteResolver> routeResolvers = new ArrayList<>();
            if (settings.getApiVersioningStrategy().getPath().isEnabled()) {
                routeResolvers.add(new PathRouteResolver(
                        settings.getApiVersioningStrategy().getPath().getVersionPrefix(), apiVersionValidator));
            }
            if (settings.getApiVersioningStrategy().getHeader().isEnabled()) {
                routeResolvers
                        .add(new HeaderRouteResolver(settings.getApiVersioningStrategy().getHeader().getHeaderName()));
            }
            if (settings.getApiVersioningStrategy().getParameter().isEnabled()) {
                routeResolvers
                        .add(new ParameterRouteResolver(
                                settings.getApiVersioningStrategy().getParameter().getParameterName(),
                                apiVersionValidator));
            }
            if (settings.getApiVersioningStrategy().getMediaTypeProfile().isEnabled()) {
                routeResolvers.add(new MediaTypeProfileRouteResolver(
                        settings.getApiVersioningStrategy().getMediaTypeProfile().getVersionPrefix(),
                        apiVersionValidator, () -> {
                            if (!settings.getApiVersioningStrategy().getMediaTypeProfile().getUriPrefix().isBlank()) {
                                return settings.getApiVersioningStrategy().getMediaTypeProfile().getUriPrefix();
                            }
                            String baseUrl = refreshableElide.getElide().getElideSettings().getBaseUrl();
                            if (StringUtils.isEmpty(baseUrl)) {
                                baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                        .path("")
                                        .build()
                                        .toUriString();
                            }
                            return baseUrl;
                        }));
            }
            if (!routeResolvers.isEmpty()) {
                return new DelegatingRouteResolver(routeResolvers);
            } else {
                return new NullRouteResolver();
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionRegistry transactionRegistry() {
        return new TransactionRegistry();
    }

    /**
     * Function which preprocesses HTTP request headers before storing them in the RequestScope.
     * @param settings Configuration settings
     * @return A function which processes HTTP Headers.
     */
    @Bean
    @ConditionalOnMissingBean
    public HeaderProcessor headerProcessor(ElideConfigProperties settings) {
        if (settings.isStripAuthorizationHeaders()) {
            return Headers::removeAuthorizationHeaders;
        } else {
            //Identity Function
            return headers -> headers;
        }
    }

    /**
     * A Set containing Types to be excluded from EntityDictionary's EntityBinding.
     * @param settings Elide configuration settings.
     * @return Set of Types.
     */
    @Bean(name = "entitiesToExclude")
    @ConditionalOnMissingBean
    public Set<Type<?>> entitiesToExclude(ElideConfigProperties settings) {
        Set<Type<?>> entitiesToExclude = new HashSet<>();

        AsyncProperties asyncProperties = settings.getAsync();

        if (asyncProperties == null || !asyncProperties.isEnabled()) {
            entitiesToExclude.add(ClassType.of(AsyncQuery.class));
        }

        boolean exportEnabled = isExportEnabled(asyncProperties);

        if (!exportEnabled) {
            entitiesToExclude.add(ClassType.of(TableExport.class));
        }

        return entitiesToExclude;
    }

    /**
     * Creates the injector for dependency injection.
     * @param beanFactory Injector to inject Elide models.
     * @return a newly configured Injector.
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public Injector injector(AutowireCapableBeanFactory beanFactory) {
        return new Injector() {
            @Override
            public void inject(Object entity) {
                beanFactory.autowireBean(entity);
            }

            @Override
            public <T> T instantiate(Class<T> cls) {
                return beanFactory.createBean(cls);
            }
        };
    }

    /**
     * Creates the default JpaDataStoreRegistrationsBuilder and applies
     * customizations.
     *
     * <p>
     * If this bean is already defined Elide will not attempt to discover
     * JpaDataStore registrations and the JpaDataStores to create can be fully
     * configured.
     *
     * <p>
     * If only minor customizations are required a
     * {@link JpaDataStoreRegistrationsBuilderCustomizer} can be defined to customize the
     * builder.
     *
     * @param applicationContext  the application context.
     * @param settings            Elide configuration settings.
     * @param optionalQueryLogger the optional query logger.
     * @param customizerProviders the customizer providers.
     * @return the default JpaDataStoreRegistrationsBuilder.
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public JpaDataStoreRegistrationsBuilder jpaDataStoreRegistrationsBuilder(
            ApplicationContext applicationContext,
            ElideConfigProperties settings,
            Optional<com.paiondata.elide.datastores.jpql.porting.QueryLogger> optionalQueryLogger,
            ObjectProvider<JpaDataStoreRegistrationsBuilderCustomizer> customizerProviders) {
        JpaDataStoreRegistrationsBuilder builder = new JpaDataStoreRegistrationsBuilder();
        String[] entityManagerFactoryNames = applicationContext.getBeanNamesForType(EntityManagerFactory.class);
        String[] platformTransactionManagerNames = applicationContext
                .getBeanNamesForType(PlatformTransactionManager.class);

        Map<String, Object> beans = new HashMap<>();
        beans.putAll(applicationContext.getBeansWithAnnotation(EnableJpaDataStore.class));
        beans.putAll(applicationContext.getBeansWithAnnotation(EnableJpaDataStores.class));
        if (!beans.isEmpty()) {
            // If there is explicit configuration
            beans.values().stream().forEach(bean -> {
                EnableJpaDataStore[] annotations = bean.getClass()
                        .getAnnotationsByType(EnableJpaDataStore.class);
                for (EnableJpaDataStore annotation : annotations) {
                    String entityManagerFactoryName = annotation.entityManagerFactoryRef();
                    String platformTransactionManagerName = annotation.transactionManagerRef();
                    if (!StringUtils.isBlank(entityManagerFactoryName)
                            && !StringUtils.isBlank(platformTransactionManagerName)) {
                        builder.add(buildJpaDataStoreRegistration(applicationContext, entityManagerFactoryName,
                                platformTransactionManagerName, settings, optionalQueryLogger,
                                annotation.managedClasses()));
                    }
                }
            });
        } else if (entityManagerFactoryNames.length == 1 && platformTransactionManagerNames.length == 1) {
            // If there is no explicit configuration but just one entity manager factory and
            // transaction manager configure it
            String platformTransactionManagerName = platformTransactionManagerNames[0];
            String entityManagerFactoryName = entityManagerFactoryNames[0];
            builder.add(buildJpaDataStoreRegistration(applicationContext, entityManagerFactoryName,
                    platformTransactionManagerName, settings, optionalQueryLogger, new Class[] {}));
        }

        customizerProviders.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

    /**
     * Creates the default DataStoreBuilder to build the DataStore and applies
     * customizations.
     * <p>
     * Override this if the default auto configured DataStores are not desirable.
     *
     * <p>
     * If only minor customizations are required a
     * {@link DataStoreBuilderCustomizer} can be defined to customize the builder.
     *
     * @param builder             JpaDataStoreRegistrationsBuilder.
     * @param settings            Elide configuration settings.
     * @param scanner             Class Scanner
     * @param optionalQueryEngine QueryEngine instance for aggregation data store.
     * @param optionalCache       Analytics query cache
     * @param optionalQueryLogger Analytics query logger
     * @param customizerProvider  Provide customizers to add to the data store
     * @return the DataStoreBuilder.
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public DataStoreBuilder dataStoreBuilder(JpaDataStoreRegistrationsBuilder builder, ElideConfigProperties settings,
            ClassScanner scanner, Optional<QueryEngine> optionalQueryEngine,
            Optional<Cache> optionalCache, Optional<QueryLogger> optionalQueryLogger,
            ObjectProvider<DataStoreBuilderCustomizer> customizerProvider) {
        return buildDataStoreBuilder(builder, settings, scanner, optionalQueryEngine, optionalCache,
                optionalQueryLogger,
                Optional.of(
                        dataStoreBuilder -> customizerProvider.orderedStream()
                                .forEach(customizer -> customizer.customize(dataStoreBuilder))));
    }

    /**
     * Creates the DataStore. Override to use a different store.
     *
     * @param dataStoreBuilder
     * @return the DataStore to be used by Elide.
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public DataStore dataStore(DataStoreBuilder dataStoreBuilder) {
        return dataStoreBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClassScanner classScanner() {
        return new DefaultClassScanner();
    }

    /**
     * Creates the default {@link ExceptionMappersBuilder} to create the {@link ExceptionMappers}.
     *
     * @param exceptionMapperRegistrationProvider the registrations
     * @param exceptionMapperProvider the exception mappers
     * @param customizerProvider the customizer
     * @return the builder
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public ExceptionMappersBuilder exceptionMappersBuilder(
            ObjectProvider<ExceptionMapperRegistration> exceptionMapperRegistrationProvider,
            ObjectProvider<ExceptionMapper<?, ?>> exceptionMapperProvider,
            ObjectProvider<ExceptionMappersBuilderCustomizer> customizerProvider) {
        ExceptionMappersBuilder exceptionMappersBuilder = BasicExceptionMappers.builder();
        // Registrations have priority
        exceptionMapperRegistrationProvider.orderedStream().forEach(exceptionMappersBuilder::register);

        exceptionMapperProvider.orderedStream().forEach(exceptionMappersBuilder::register);

        customizerProvider.orderedStream().forEach(customizer -> customizer.customize(exceptionMappersBuilder));
        return exceptionMappersBuilder;
    }

    /**
     * Creates the default {@link ExceptionMappersBuilder}.
     *
     * @param exceptionMappersBuilder the builder
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public ExceptionMappers exceptionMapper(ExceptionMappersBuilder exceptionMappersBuilder) {
        return exceptionMappersBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionLogger exceptionLogger() {
        return new Slf4jExceptionLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public ObjectMapperBuilder objectMapperBuilder(
            Optional<Jackson2ObjectMapperBuilder> optionalJackson2ObjectMapperBuilder) {
        if (optionalJackson2ObjectMapperBuilder.isPresent()) {
            return optionalJackson2ObjectMapperBuilder.get()::build;
        }
        return ObjectMapper::new;
    }

    @Bean
    @ConditionalOnMissingBean
    public ElideMapper elideMapper(ObjectMapperBuilder builder) {
        return new ElideMapper(builder.build());
    }

    @Configuration
    @ConditionalOnClass({ OpenApiCustomizer.class, OpenApiBuilder.class })
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
    public static class SpringDocConfiguration {
        /**
         * Creates a SpringDoc OpenApiCustomizer for Elide to add all the models to.
         * This can only expose the default version.
         * @param elide    Singleton elide instance.
         * @return
         */
        @Bean
        @ConditionalOnMissingBean
        public ElideOpenApiCustomizer elideOpenApiCustomizer(RefreshableElide elide, ElideConfigProperties properties) {
            return new DefaultElideOpenApiCustomizer(elide, properties);
        }

        @Bean
        public ElideGroupedOpenApiCustomizer elideGroupedOpenApiCustomizer(RefreshableElide elide,
                ElideConfigProperties properties) {
            return new DefaultElideGroupedOpenApiCustomizer(elide, properties);
        }

        @Configuration
        public static class ElideGroupedOpenApiConfiguration {
            public ElideGroupedOpenApiConfiguration(Optional<List<GroupedOpenApi>> optionalGroupedOpenApis,
                    ObjectProvider<ElideGroupedOpenApiCustomizer> customizerProvider) {
                optionalGroupedOpenApis.ifPresent(groupedOpenApis -> {
                    for (GroupedOpenApi groupedOpenApi : groupedOpenApis) {
                        customizerProvider.orderedStream().forEach(customizer -> customizer.customize(groupedOpenApi));
                    }
                });
            }
        }
    }

    @Configuration
    @ConditionalOnClass(RefreshScope.class)
    @ConditionalOnProperty(name = "spring.cloud.refresh.enabled", havingValue = "true", matchIfMissing = true)
    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    public static class RefreshableConfiguration {
        /**
         * Creates the entity dictionary for Elide which contains static metadata about Elide models.
         * Override to load check classes or life cycle hooks.
         * @param entityDictionaryBuilder the builder
         * @return a newly configured EntityDictionary.
         */
        @Bean
        @RefreshScope
        @ConditionalOnMissingBean
        public EntityDictionary entityDictionary(EntityDictionaryBuilder entityDictionaryBuilder) {
            return entityDictionaryBuilder.build();
        }

        /**
         * Creates the Elide instance with standard settings.
         * @param elideSettingsBuilder the builder.
         * @param dictionary Stores the static metadata about Elide models.
         * @param transactionRegistry Global transaction registry.
         * @return A new elide instance.
         */
        @Bean
        @RefreshScope
        @ConditionalOnMissingBean
        public RefreshableElide refreshableElide(ElideSettingsBuilder elideSettingsBuilder, EntityDictionary dictionary,
                TransactionRegistry transactionRegistry) {
            return buildRefreshableElide(elideSettingsBuilder, dictionary, transactionRegistry);
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.json-api.enabled", havingValue = "true")
        public static class JsonApiConfiguration {
            @Bean
            @RefreshScope
            @ConditionalOnMissingBean(name = "jsonApiController")
            public JsonApiController jsonApiController(JsonApi jsonApi, ElideConfigProperties settings,
                    RouteResolver routeResolver) {
                return new JsonApiController(jsonApi, settings, routeResolver);
            }

            @Bean
            @RefreshScope
            @ConditionalOnMissingBean
            public JsonApi jsonApi(RefreshableElide refreshableElide) {
                return new JsonApi(refreshableElide);
            }
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.api-docs.enabled", havingValue = "true")
        @ConditionalOnClass(OpenApiBuilder.class)
        public static class ApiDocsConfiguration {
            /**
             * Creates a singular openapi document for JSON-API.
             * @param elide Singleton elide instance.
             * @param settings Elide configuration settings.
             * @param customizer Customizer to customize the OpenAPI document.
             * @return An instance of a JPA DataStore.
             */
            @Bean
            @RefreshScope
            @ConditionalOnMissingBean
            public ApiDocsController.ApiDocsRegistrations apiDocsRegistrations(RefreshableElide elide,
                    ElideConfigProperties settings, ServerProperties serverProperties,
                    OpenApiDocumentCustomizer customizer) {
                return buildApiDocsRegistrations(elide, settings, serverProperties.getServlet().getContextPath(),
                        customizer);
            }

            @Bean
            @RefreshScope
            @ConditionalOnMissingBean(name = "apiDocsController")
            public ApiDocsController apiDocsController(ApiDocsController.ApiDocsRegistrations docs,
                    RouteResolver routeResolver, ElideConfigProperties elideConfigProperties) {
                return new ApiDocsController(docs, routeResolver, elideConfigProperties);
            }

            @Bean
            @RefreshScope
            @ConditionalOnMissingBean
            public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
                return new BasicOpenApiDocumentCustomizer();
            }
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
        @ConditionalOnClass(GraphQLSettingsBuilder.class)
        public static class GraphQLConfiguration {
            @Bean
            @RefreshScope
            @ConditionalOnMissingBean
            public QueryRunners queryRunners(RefreshableElide refreshableElide,
                    DataFetcherExceptionHandler exceptionHandler) {
                return new QueryRunners(refreshableElide, exceptionHandler);
            }

            @Bean
            @RefreshScope
            @ConditionalOnMissingBean(name = "graphqlController")
            public GraphqlController graphqlController(RefreshableElide refreshableElide, QueryRunners runners,
                    ElideMapper elideMapper, HeaderProcessor headerProcessor, ElideConfigProperties settings,
                    RouteResolver routeResolver) {
                return new GraphqlController(refreshableElide.getElide(), runners, headerProcessor, settings,
                        routeResolver);
            }
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "spring.cloud.refresh.enabled", havingValue = "false", matchIfMissing = true)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public static class NonRefreshableConfiguration {
        /**
         * Creates the entity dictionary for Elide which contains static metadata about Elide models.
         * Override to load check classes or life cycle hooks.
         * @param entityDictionaryBuilder the builder
         * @return a newly configured EntityDictionary.
         */
        @Bean
        @ConditionalOnMissingBean
        public EntityDictionary entityDictionary(EntityDictionaryBuilder entityDictionaryBuilder) {
            return entityDictionaryBuilder.build();
        }

        /**
         * Creates the Elide instance with standard settings.
         * @param elideSettingsBuilder the builder.
         * @param dictionary Stores the static metadata about Elide models.
         * @param transactionRegistry Global transaction registry.
         * @return A new elide instance.
         */
        @Bean
        @ConditionalOnMissingBean
        public RefreshableElide refreshableElide(ElideSettingsBuilder elideSettingsBuilder, EntityDictionary dictionary,
                TransactionRegistry transactionRegistry) {
            return buildRefreshableElide(elideSettingsBuilder, dictionary, transactionRegistry);
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.json-api.enabled", havingValue = "true")
        public static class JsonApiConfiguration {
            @Bean
            @ConditionalOnMissingBean(name = "jsonApiController")
            public JsonApiController jsonApiController(JsonApi jsonApi, ElideConfigProperties settings,
                    RouteResolver routeResolver) {
                return new JsonApiController(jsonApi, settings, routeResolver);
            }

            @Bean
            @ConditionalOnMissingBean
            public JsonApi jsonApi(RefreshableElide refreshableElide) {
                return new JsonApi(refreshableElide);
            }
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.api-docs.enabled", havingValue = "true")
        @ConditionalOnClass(OpenApiBuilder.class)
        public static class ApiDocsConfiguration {
            /**
             * Creates a singular openapi document for JSON-API.
             * @param elide Singleton elide instance.
             * @param settings Elide configuration settings.
             * @param customizer Customizer to customize the OpenAPI document.
             * @return An instance of a JPA DataStore.
             */
            @Bean
            @ConditionalOnMissingBean
            public ApiDocsController.ApiDocsRegistrations apiDocsRegistrations(RefreshableElide elide,
                    ElideConfigProperties settings, ServerProperties serverProperties,
                    OpenApiDocumentCustomizer customizer) {
                return buildApiDocsRegistrations(elide, settings, serverProperties.getServlet().getContextPath(),
                        customizer);
            }

            @Bean
            @ConditionalOnMissingBean(name = "apiDocsController")
            public ApiDocsController apiDocsController(ApiDocsController.ApiDocsRegistrations docs,
                    RouteResolver routeResolver, ElideConfigProperties elideConfigProperties) {
                return new ApiDocsController(docs, routeResolver, elideConfigProperties);
            }

            @Bean
            @ConditionalOnMissingBean
            public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
                return new BasicOpenApiDocumentCustomizer();
            }
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
        @ConditionalOnClass(GraphQLSettingsBuilder.class)
        public static class GraphQLConfiguration {
            @Bean
            @ConditionalOnMissingBean
            public QueryRunners queryRunners(RefreshableElide refreshableElide,
                    DataFetcherExceptionHandler exceptionHandler) {
                return new QueryRunners(refreshableElide, exceptionHandler);
            }

            @Bean
            @ConditionalOnMissingBean(name = "graphqlController")
            public GraphqlController graphqlController(RefreshableElide refreshableElide, QueryRunners runners,
                    ElideMapper elideMapper, HeaderProcessor headerProcessor, ElideConfigProperties settings,
                    RouteResolver routeResolver) {
                return new GraphqlController(refreshableElide.getElide(), runners, headerProcessor, settings,
                        routeResolver);
            }
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
    @ConditionalOnClass(GraphQLSettingsBuilder.class)
    public static class GraphQLConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @Scope(SCOPE_PROTOTYPE)
        public DataFetcherExceptionHandler dataFetcherExceptionHandler() {
            return new SimpleDataFetcherExceptionHandler();
        }

        @Bean
        @ConditionalOnMissingBean
        public GraphQLErrorMapper graphqlErrorMapper() {
            return new DefaultGraphQLErrorMapper();
        }

        @Bean
        @ConditionalOnMissingBean
        public GraphQLExceptionHandler graphqlExceptionHandler(ExceptionLogger exceptionLogger,
                ExceptionMappers exceptionMappers, GraphQLErrorMapper graphqlErrorMapper) {
            return new DefaultGraphQLExceptionHandler(exceptionLogger, exceptionMappers, graphqlErrorMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        @Scope(SCOPE_PROTOTYPE)
        public GraphQLSettingsBuilder graphqlSettingsBuilder(ElideConfigProperties settings,
                EntityDictionary entityDictionary,
                GraphQLExceptionHandler graphqlExceptionHandler,
                ObjectProvider<GraphQLSettingsBuilderCustomizer> customizerProviders) {
            return GraphQLSettingsBuilderCustomizers.buildGraphQLSettingsBuilder(entityDictionary,
                    builder -> {
                        builder.path(settings.getGraphql().getPath())
                                .federation(federation -> federation
                                        .enabled(settings.getGraphql().getFederation().isEnabled())
                                        .version(settings.getGraphql().getFederation().getVersion().getValue()))
                                .graphqlExceptionHandler(graphqlExceptionHandler);
                        customizerProviders.orderedStream().forEach(customizer -> customizer.customize(builder));
                    });
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "elide.jsonapi.enabled", havingValue = "true")
    @ConditionalOnClass(JsonApiSettingsBuilder.class)
    public static class JsonApiConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public JsonApiErrorMapper jsonApiErrorMapper() {
            return new DefaultJsonApiErrorMapper();
        }

        @Bean
        @ConditionalOnMissingBean
        public JsonApiExceptionHandler jsonApiExceptionHandler(ExceptionLogger exceptionLogger,
                ExceptionMappers exceptionMappers, JsonApiErrorMapper jsonApiErrorMapper) {
            return new DefaultJsonApiExceptionHandler(exceptionLogger, exceptionMappers, jsonApiErrorMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        @Scope(SCOPE_PROTOTYPE)
        public JsonApiSettingsBuilder jsonApiSettingsBuilder(ElideConfigProperties settings,
                EntityDictionary entityDictionary, JsonApiMapper jsonApiMapper,
                JsonApiExceptionHandler jsonApiExceptionHandler,
                ObjectProvider<JsonApiSettingsBuilderCustomizer> customizerProviders) {
            return JsonApiSettingsBuilderCustomizers.buildJsonApiSettingsBuilder(entityDictionary, builder -> {
                builder.path(settings.getJsonApi().getPath())
                        .joinFilterDialect(RSQLFilterDialect.builder().dictionary(entityDictionary).build())
                        .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(entityDictionary).build())
                        .jsonApiMapper(jsonApiMapper).jsonApiExceptionHandler(jsonApiExceptionHandler);
                if (settings.getJsonApi().getLinks().isEnabled()) {
                    String baseUrl = settings.getBaseUrl();
                    builder.links(links -> links.enabled(true));
                    if (StringUtils.isEmpty(baseUrl)) {
                        builder.links(links -> links.jsonApiLinks(new DefaultJsonApiLinks()));
                    } else {
                        String jsonApiBaseUrl = baseUrl + settings.getJsonApi().getPath() + "/";
                        builder.links(links -> links.jsonApiLinks(new DefaultJsonApiLinks(jsonApiBaseUrl)));
                    }
                }
                customizerProviders.orderedStream().forEach(customizer -> customizer.customize(builder));
            });
        }

        @Bean
        @ConditionalOnMissingBean
        public JsonApiMapper jsonApiMapper(ElideMapper elideMapper) {
            return new JsonApiMapper(elideMapper.getObjectMapper());
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "elide.async.enabled", havingValue = "true")
    @ConditionalOnClass(AsyncSettingsBuilder.class)
    public static class AsyncConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @Scope(SCOPE_PROTOTYPE)
        public AsyncSettingsBuilder asyncSettingsBuilder(ElideConfigProperties settings,
                ObjectProvider<AsyncSettingsBuilderCustomizer> customizerProviders) {
            return AsyncSettingsBuilderCustomizers.buildAsyncSettingsBuilder(builder -> {
                builder.export(export -> export.enabled(settings.getAsync().getExport().isEnabled())
                        .path(settings.getAsync().getExport().getPath()));
                customizerProviders.orderedStream().forEach(customizer -> customizer.customize(builder));
            });
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = "elide.async.export.enabled", havingValue = "true")
        public ExportController exportController(ResultStorageEngine resultStorageEngine) {
            return new ExportController(resultStorageEngine);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    public static class AggregationStoreConfiguration {
        /**
         * Creates dynamic configuration for models, security roles, and database connections.
         * @param settings Config Settings.
         * @throws IOException if there is an error reading the configuration.
         * @return An instance of DynamicConfiguration.
         */
        @Bean
        @Scope(SCOPE_PROTOTYPE)
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = "elide.aggregation-store.dynamic-config.enabled", havingValue = "true")
        public DynamicConfiguration dynamicConfiguration(ClassScanner scanner,
                                                              ElideConfigProperties settings) throws IOException {
            DynamicConfigValidator validator = new DynamicConfigValidator(scanner,
                    settings.getAggregationStore().getDynamicConfig().getPath());
            validator.readAndValidateConfigs();
            return validator;
        }

        /**
         * Creates the default Password Extractor Implementation.
         * @return An instance of DBPasswordExtractor.
         */
        @Bean
        @ConditionalOnMissingBean
        public DBPasswordExtractor dbPasswordExtractor() {
            return config -> StringUtils.EMPTY;
        }

        /**
         * Provides the default Hikari DataSource Configuration.
         * @return An instance of DataSourceConfiguration.
         */
        @Bean
        @ConditionalOnMissingBean
        public DataSourceConfiguration dataSourceConfiguration() {
            return new DataSourceConfiguration() {
            };
        }

        /**
         * Create a QueryEngine instance for aggregation data store to use.
         * @param defaultDataSource DataSource for JPA.
         * @param optionalDynamicConfig An instance of DynamicConfiguration.
         * @param settings Elide configuration settings.
         * @param dataSourceConfiguration DataSource Configuration
         * @param dbPasswordExtractor Password Extractor Implementation
         * @return An instance of a QueryEngine
         */
        @Bean
        @ConditionalOnMissingBean
        @Scope(SCOPE_PROTOTYPE)
        public QueryEngine queryEngine(DataSource defaultDataSource,
                                            Optional<DynamicConfiguration> optionalDynamicConfig,
                                            ElideConfigProperties settings,
                                            ClassScanner scanner,
                                            DataSourceConfiguration dataSourceConfiguration,
                                            DBPasswordExtractor dbPasswordExtractor) {

            boolean enableMetaDataStore = settings.getAggregationStore().getMetadataStore().isEnabled();
            ConnectionDetails defaultConnectionDetails = new ConnectionDetails(defaultDataSource,
                            SQLDialectFactory.getDialect(settings.getAggregationStore().getDefaultDialect()));
            if (isDynamicConfigEnabled(settings) && optionalDynamicConfig.isPresent()) {
                DynamicConfiguration dynamicConfig = optionalDynamicConfig.get();
                MetaDataStore metaDataStore = new MetaDataStore(scanner, dynamicConfig.getTables(),
                        dynamicConfig.getNamespaceConfigurations(), enableMetaDataStore);

                Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();

                dynamicConfig.getDatabaseConfigurations().forEach(dbConfig ->
                    connectionDetailsMap.put(dbConfig.getName(),
                            new ConnectionDetails(
                                    dataSourceConfiguration.getDataSource(dbConfig, dbPasswordExtractor),
                                    SQLDialectFactory.getDialect(dbConfig.getDialect())))
                );

                Function<String, ConnectionDetails> connectionDetailsLookup = name -> {
                    if (StringUtils.isEmpty(name)) {
                        return defaultConnectionDetails;
                    }
                    return Optional.ofNullable(connectionDetailsMap.get(name))
                            .orElseThrow(() -> new IllegalStateException("ConnectionDetails undefined for connection: "
                                    + name));
                };

                return new SQLQueryEngine(metaDataStore, connectionDetailsLookup,
                        new HashSet<>(Arrays.asList(new AggregateBeforeJoinOptimizer(metaDataStore))),
                        new DefaultQueryPlanMerger(metaDataStore),
                        new DefaultQueryValidator(metaDataStore.getMetadataDictionary()));
            }
            MetaDataStore metaDataStore = new MetaDataStore(scanner, enableMetaDataStore);
            return new SQLQueryEngine(metaDataStore, unused -> defaultConnectionDetails);
        }

        /**
         * Creates a query result cache to be used by {@link #dataStore}, or null if cache is to be disabled.
         * @param settings Elide configuration settings.
         * @param optionalMeterRegistry Meter Registry.
         * @return An instance of a query cache, or null.
         */
        @Bean
        @ConditionalOnMissingBean
        public Cache queryCache(ElideConfigProperties settings, Optional<MeterRegistry> optionalMeterRegistry) {
            int maxCacheItems = settings.getAggregationStore().getQueryCache().getMaxSize();
            if (settings.getAggregationStore().getQueryCache().isEnabled() && maxCacheItems > 0) {
                final CaffeineCache cache = new CaffeineCache(maxCacheItems,
                        settings.getAggregationStore().getQueryCache().getExpiration());
                optionalMeterRegistry.ifPresent(meterRegistry -> CaffeineCacheMetrics.monitor(meterRegistry,
                        cache.getImplementation(), "elideQueryCache"));
                return cache;
            }
            return null;
        }

        /**
         * Creates a querylogger to be used by {@link #dataStore} for aggregation.
         * @return The default Noop QueryLogger.
         */
        @Bean
        @ConditionalOnMissingBean
        public QueryLogger queryLogger() {
            return new Slf4jQueryLogger();
        }
    }

    public static RefreshableElide buildRefreshableElide(ElideSettingsBuilder elideSettingsBuilder,
            EntityDictionary dictionary, TransactionRegistry transactionRegistry) {
        Elide elide = new Elide(elideSettingsBuilder.build(), transactionRegistry, dictionary.getScanner(), true);
        return new RefreshableElide(elide);
    }

    public static ApiDocsController.ApiDocsRegistrations buildApiDocsRegistrations(RefreshableElide elide,
            ElideConfigProperties settings, String contextPath, OpenApiDocumentCustomizer customizer) {
        String jsonApiPath = settings.getJsonApi() != null ? settings.getJsonApi().getPath() : "";

        EntityDictionary dictionary = elide.getElide().getElideSettings().getEntityDictionary();

        List<ApiDocsRegistration> registrations = new ArrayList<>();
        dictionary.getApiVersions().stream().forEach(apiVersion -> {
            Supplier<OpenAPI> document = () -> {
                OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(apiVersion)
                        .supportLegacyFilterDialect(false);
                if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                    if (settings.getApiVersioningStrategy().getPath().isEnabled()) {
                        // Path needs to be set
                        builder.basePath(
                                "/" + settings.getApiVersioningStrategy().getPath().getVersionPrefix() + apiVersion);
                    } else if (settings.getApiVersioningStrategy().getHeader().isEnabled()) {
                        // Header needs to be set
                        builder.globalParameter(new Parameter().in("header")
                                .name(settings.getApiVersioningStrategy().getHeader().getHeaderName()[0]).required(true)
                                .schema(new StringSchema().addEnumItem(apiVersion)));
                    } else if (settings.getApiVersioningStrategy().getParameter().isEnabled()) {
                        // Header needs to be set
                        builder.globalParameter(new Parameter().in("query")
                                .name(settings.getApiVersioningStrategy().getParameter().getParameterName())
                                .required(true).schema(new StringSchema().addEnumItem(apiVersion)));
                    }
                }
                String url = contextPath != null ? contextPath : "";
                url = url + jsonApiPath;
                if (url.isBlank()) {
                    url = "/";
                }
                OpenAPI openApi = builder.build();
                openApi.addServersItem(new Server().url(url));
                if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                    Info info = openApi.getInfo();
                    if (info == null) {
                        info = new Info();
                        openApi.setInfo(info);
                    }
                    info.setVersion(apiVersion);
                }
                customizer.customize(openApi);
                return openApi;
            };
            registrations.add(new ApiDocsRegistration("", SingletonSupplier.of(document),
                    settings.getApiDocs().getVersion().getValue(), apiVersion));
        });
        return new ApiDocsController.ApiDocsRegistrations(registrations);
    }

    /**
     * Creates the default DataStoreBuilder to build the DataStore.
     * @param builder JpaDataStoreRegistrationsBuilder.
     * @param settings Elide configuration settings.
     * @param scanner Class Scanner
     * @param optionalQueryEngine QueryEngine instance for aggregation data store.
     * @param optionalCache Analytics query cache
     * @param optionalQueryLogger Analytics query logger
     * @param optionalCustomizer Provide customizers to add to the data store
     * @return the DataStoreBuilder.
     */
    public static DataStoreBuilder buildDataStoreBuilder(JpaDataStoreRegistrationsBuilder builder,
            ElideConfigProperties settings,
            ClassScanner scanner,
            Optional<QueryEngine> optionalQueryEngine, Optional<Cache> optionalCache,
            Optional<QueryLogger> optionalQueryLogger,
            Optional<DataStoreBuilderCustomizer> optionalCustomizer) {
        DataStoreBuilder dataStoreBuilder = new DataStoreBuilder();

        builder.build().forEach(registration -> {
            if (registration.getManagedClasses() != null && !registration.getManagedClasses().isEmpty()) {
                dataStoreBuilder.dataStore(new JpaDataStore(registration.getEntityManagerSupplier(),
                        registration.getReadTransactionSupplier(), registration.getWriteTransactionSupplier(),
                        registration.getQueryLogger(), registration.getManagedClasses().toArray(Type<?>[]::new)));
            } else {
                dataStoreBuilder.dataStore(new JpaDataStore(registration.getEntityManagerSupplier(),
                        registration.getReadTransactionSupplier(), registration.getWriteTransactionSupplier(),
                        registration.getQueryLogger(), registration.getMetamodelSupplier()));
            }
        });

        if (isAggregationStoreEnabled(settings)) {
            AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = AggregationDataStore
                    .builder();
            optionalQueryEngine.ifPresent(aggregationDataStoreBuilder::queryEngine);

            if (isDynamicConfigEnabled(settings)) {
                optionalQueryEngine.ifPresent(queryEngine -> aggregationDataStoreBuilder
                        .dynamicCompiledClasses(queryEngine.getMetaDataStore().getDynamicTypes()));
                if (settings.getAggregationStore().getDynamicConfig().getConfigApi().isEnabled()) {
                    dataStoreBuilder
                            .dataStore(new ConfigDataStore(settings.getAggregationStore().getDynamicConfig().getPath(),
                                    new TemplateConfigValidator(scanner,
                                            settings.getAggregationStore().getDynamicConfig().getPath())));
                }
            }
            optionalCache.ifPresent(aggregationDataStoreBuilder::cache);
            optionalQueryLogger.ifPresent(aggregationDataStoreBuilder::queryLogger);
            AggregationDataStore aggregationDataStore = aggregationDataStoreBuilder.build();

            // meta data store needs to be put at first to populate meta data models
            optionalQueryEngine.ifPresent(queryEngine -> dataStoreBuilder.dataStore(queryEngine.getMetaDataStore()));
            dataStoreBuilder.dataStore(aggregationDataStore);
        }
        optionalCustomizer.ifPresent(customizer -> customizer.customize(dataStoreBuilder));
        return dataStoreBuilder;
    }

    /**
     * Creates a JpaDataStore registration from inputs.
     *
     * @param applicationContext the application context
     * @param entityManagerFactoryName the bean name of the entity manager factory
     * @param platformTransactionManagerName the bean name of the platform transaction manager
     * @param settings the settings
     * @param optionalQueryLogger the optional query logger
     * @return the JpaDataStoreRegistration read from the application context.
     */
    private JpaDataStoreRegistration buildJpaDataStoreRegistration(ApplicationContext applicationContext,
            String entityManagerFactoryName, String platformTransactionManagerName, ElideConfigProperties settings,
            Optional<com.paiondata.elide.datastores.jpql.porting.QueryLogger> optionalQueryLogger,
            Class<?>[] managedClasses) {
        PlatformTransactionManager platformTransactionManager = applicationContext
                .getBean(platformTransactionManagerName, PlatformTransactionManager.class);
        EntityManagerFactory entityManagerFactory = applicationContext.getBean(entityManagerFactoryName,
                EntityManagerFactory.class);
        return JpaDataStoreRegistrations.buildJpaDataStoreRegistration(entityManagerFactoryName, entityManagerFactory,
                platformTransactionManagerName, platformTransactionManager, settings, optionalQueryLogger,
                managedClasses);
    }

    public static boolean isDynamicConfigEnabled(ElideConfigProperties settings) {

        boolean enabled = false;
        if (settings.getAggregationStore() != null && settings.getAggregationStore().getDynamicConfig() != null) {
            enabled = settings.getAggregationStore().getDynamicConfig().isEnabled();
        }

        return enabled;

    }

    public static boolean isAggregationStoreEnabled(ElideConfigProperties settings) {

        boolean enabled = false;
        if (settings.getAggregationStore() != null) {
            enabled = settings.getAggregationStore().isEnabled();
        }

        return enabled;

    }

    public static boolean isExportEnabled(AsyncProperties asyncProperties) {

        return asyncProperties != null && asyncProperties.getExport() != null
                && asyncProperties.getExport().isEnabled();
    }
}
