/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.request.route.BasicApiVersionValidator;
import com.yahoo.elide.core.request.route.FlexibleRouteResolver;
import com.yahoo.elide.core.request.route.NullRouteResolver;
import com.yahoo.elide.core.request.route.RouteResolver;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.CaffeineCache;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.DataSourceConfiguration;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.AggregateBeforeJoinOptimizer;
import com.yahoo.elide.datastores.aggregation.validator.TemplateConfigValidator;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.graphql.QueryRunners;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.links.DefaultJsonApiLinks;
import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import com.yahoo.elide.modelconfig.DynamicConfiguration;
import com.yahoo.elide.modelconfig.store.ConfigDataStore;
import com.yahoo.elide.modelconfig.store.models.ConfigChecks;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import com.yahoo.elide.spring.api.BasicOpenApiDocumentCustomizer;
import com.yahoo.elide.spring.api.DefaultElideOpenApiCustomizer;
import com.yahoo.elide.spring.api.ElideOpenApiCustomizer;
import com.yahoo.elide.spring.api.OpenApiDocumentCustomizer;
import com.yahoo.elide.spring.controllers.ApiDocsController;
import com.yahoo.elide.spring.controllers.ApiDocsController.ApiDocsRegistration;
import com.yahoo.elide.spring.controllers.ExportController;
import com.yahoo.elide.spring.controllers.GraphqlController;
import com.yahoo.elide.spring.controllers.JsonApiController;
import com.yahoo.elide.spring.datastore.config.DataStoreBuilder;
import com.yahoo.elide.spring.datastore.config.DataStoreBuilderCustomizer;
import com.yahoo.elide.spring.jackson.ObjectMapperBuilder;
import com.yahoo.elide.spring.orm.jpa.config.EnableJpaDataStore;
import com.yahoo.elide.spring.orm.jpa.config.EnableJpaDataStores;
import com.yahoo.elide.spring.orm.jpa.config.JpaDataStoreRegistration;
import com.yahoo.elide.spring.orm.jpa.config.JpaDataStoreRegistrations;
import com.yahoo.elide.spring.orm.jpa.config.JpaDataStoreRegistrationsBuilder;
import com.yahoo.elide.spring.orm.jpa.config.JpaDataStoreRegistrationsBuilderCustomizer;
import com.yahoo.elide.swagger.OpenApiBuilder;
import com.yahoo.elide.utils.HeaderUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
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
     * Creates dynamic configuration for models, security roles, and database connections.
     * @param settings Config Settings.
     * @throws IOException if there is an error reading the configuration.
     * @return An instance of DynamicConfiguration.
     */
    @Bean
    @Scope(SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean
    @ConditionalOnExpression(
            "${elide.aggregation-store.enabled:false} and ${elide.aggregation-store.dynamic-config.enabled:false}")
    public DynamicConfiguration buildDynamicConfiguration(ClassScanner scanner,
                                                          ElideConfigProperties settings) throws IOException {
        DynamicConfigValidator validator = new DynamicConfigValidator(scanner,
                settings.getAggregationStore().getDynamicConfig().getPath());
        validator.readAndValidateConfigs();
        return validator;
    }

    @Bean
    @ConditionalOnMissingBean
    public RouteResolver routeResolver(RefreshableElide refreshableElide) {
        Set<String> apiVersions = refreshableElide.getElide().getElideSettings().getDictionary().getApiVersions();
        if (apiVersions.size() == 1 && apiVersions.contains(EntityDictionary.NO_VERSION)) {
            return new NullRouteResolver();
        } else {
            return new FlexibleRouteResolver(new BasicApiVersionValidator(), () -> {
                String baseUrl = refreshableElide.getElide().getElideSettings().getBaseUrl();
                String prefix = refreshableElide.getElide().getElideSettings().getJsonApiPath();

                if (StringUtils.isEmpty(baseUrl)) {
                    baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                }

                if (prefix.length() > 1) {
                    if (baseUrl.endsWith("/")) {
                        baseUrl = baseUrl.substring(0, baseUrl.length() - 1) + prefix;
                    } else {
                        baseUrl = baseUrl + prefix;
                    }
                }

                return baseUrl;
            });
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionRegistry createRegistry() {
        return new TransactionRegistry();
    }

    /**
     * Creates the default Password Extractor Implementation.
     * @return An instance of DBPasswordExtractor.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    public DBPasswordExtractor getDBPasswordExtractor() {
        return config -> StringUtils.EMPTY;
    }

    /**
     * Provides the default Hikari DataSource Configuration.
     * @return An instance of DataSourceConfiguration.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    public DataSourceConfiguration getDataSourceConfiguration() {
        return new DataSourceConfiguration() {
        };
    }

    @Configuration
    @ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
    public static class GraphQLConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @Scope(SCOPE_PROTOTYPE)
        public DataFetcherExceptionHandler getGraphQLExceptionHandler() {
            return new SimpleDataFetcherExceptionHandler();
        }
    }

    /**
     * Function which preprocesses HTTP request headers before storing them in the RequestScope.
     * @param settings Configuration settings
     * @return A function which processes HTTP Headers.
     */
    @Bean
    @ConditionalOnMissingBean
    public HeaderUtils.HeaderProcessor getHeaderProcessor(ElideConfigProperties settings) {
        if (settings.isStripAuthorizationHeaders()) {
            return HeaderUtils::lowercaseAndRemoveAuthHeaders;
        } else {
            //Identity Function
            return (a) -> a;
        }
    }

    /**
     * A Set containing Types to be excluded from EntityDictionary's EntityBinding.
     * @param settings Elide configuration settings.
     * @return Set of Types.
     */
    @Bean(name = "entitiesToExclude")
    @ConditionalOnMissingBean
    public Set<Type<?>> getEntitiesToExclude(ElideConfigProperties settings) {
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
    public Injector buildInjector(AutowireCapableBeanFactory beanFactory) {
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
     * Creates the entity dictionary for Elide which contains static metadata about Elide models.
     * Override to load check classes or life cycle hooks.
     * @param injector Injector to inject Elide models.
     * @param optionalDynamicConfig An instance of DynamicConfiguration.
     * @param settings Elide configuration settings.
     * @param entitiesToExclude set of Entities to exclude from binding.
     * @return a newly configured EntityDictionary.
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public EntityDictionary buildDictionary(Injector injector,
                                            ClassScanner scanner,
                                            Optional<DynamicConfiguration> optionalDynamicConfig,
                                            ElideConfigProperties settings,
                                            @Qualifier("entitiesToExclude") Set<Type<?>> entitiesToExclude) {

        Map<String, Class<? extends Check>> checks = new HashMap<>();

        if (settings.getAggregationStore().getDynamicConfig().getConfigApi().isEnabled()) {
            checks.put(ConfigChecks.CAN_CREATE_CONFIG, ConfigChecks.CanNotCreate.class);
            checks.put(ConfigChecks.CAN_READ_CONFIG, ConfigChecks.CanNotRead.class);
            checks.put(ConfigChecks.CAN_DELETE_CONFIG, ConfigChecks.CanNotDelete.class);
            checks.put(ConfigChecks.CAN_UPDATE_CONFIG, ConfigChecks.CanNotUpdate.class);
        }

        EntityDictionary dictionary = new EntityDictionary(
                checks, //Checks
                new HashMap<>(), //Role Checks
                injector,
                CoerceUtil::lookup, //Serde Lookup
                entitiesToExclude,
                scanner);

        if (isAggregationStoreEnabled(settings) && isDynamicConfigEnabled(settings)) {
            optionalDynamicConfig.ifPresent(dynamicConfig ->
                dynamicConfig.getRoles().forEach(role ->
                    dictionary.addRoleCheck(role, new Role.RoleMemberCheck(role))
                )
            );
        }

        return dictionary;
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
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    @Scope(SCOPE_PROTOTYPE)
    public QueryEngine buildQueryEngine(DataSource defaultDataSource,
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
            Optional<com.yahoo.elide.datastores.jpql.porting.QueryLogger> optionalQueryLogger,
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
            Optional<com.yahoo.elide.datastores.jpql.porting.QueryLogger> optionalQueryLogger,
            Class<?>[] managedClasses) {
        PlatformTransactionManager platformTransactionManager = applicationContext
                .getBean(platformTransactionManagerName, PlatformTransactionManager.class);
        EntityManagerFactory entityManagerFactory = applicationContext.getBean(entityManagerFactoryName,
                EntityManagerFactory.class);
        return JpaDataStoreRegistrations.buildJpaDataStoreRegistration(entityManagerFactoryName, entityManagerFactory,
                platformTransactionManagerName, platformTransactionManager, settings, optionalQueryLogger,
                managedClasses);
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
     * Creates a query result cache to be used by {@link #dataStore}, or null if cache is to be disabled.
     * @param settings Elide configuration settings.
     * @param optionalMeterRegistry Meter Registry.
     * @return An instance of a query cache, or null.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    public Cache buildQueryCache(ElideConfigProperties settings, Optional<MeterRegistry> optionalMeterRegistry) {
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
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    public QueryLogger buildQueryLogger() {
        return new Slf4jQueryLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClassScanner getClassScanner() {
        return new DefaultClassScanner();
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorMapper getErrorMapper() {
        return error -> null;
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
    @Scope(SCOPE_PROTOTYPE)
    public JsonApiMapper jsonApiMapper(ObjectMapperBuilder builder) {
        return new JsonApiMapper(builder.build());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${elide.async.enabled:false} && ${elide.async.export.enabled:false}")
    public ExportController exportController(ResultStorageEngine resultStorageEngine) {
        return new ExportController(resultStorageEngine);
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
        @Scope(SCOPE_PROTOTYPE)
        public ElideOpenApiCustomizer elideOpenApiCustomizer(RefreshableElide elide) {
            return new DefaultElideOpenApiCustomizer(elide, EntityDictionary.NO_VERSION);
        }
    }

    @Configuration
    @ConditionalOnClass(RefreshScope.class)
    @ConditionalOnProperty(name = "spring.cloud.refresh.enabled", havingValue = "true", matchIfMissing = true)
    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    public static class RefreshableConfiguration {
        /**
         * Creates the Elide instance with standard settings.
         * @param dictionary Stores the static metadata about Elide models.
         * @param dataStore The persistence store.
         * @param headerProcessor HTTP header function which is invoked for every request.
         * @param transactionRegistry Global transaction registry.
         * @param settings Elide settings.
         * @return A new elide instance.
         */
        @Bean
        @RefreshScope
        @ConditionalOnMissingBean
        public RefreshableElide refreshableElide(EntityDictionary dictionary, DataStore dataStore,
                HeaderUtils.HeaderProcessor headerProcessor, TransactionRegistry transactionRegistry,
                ElideConfigProperties settings, JsonApiMapper mapper, ErrorMapper errorMapper) {
            return buildRefreshableElide(dictionary, dataStore, headerProcessor, transactionRegistry, settings, mapper,
                    errorMapper);
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.json-api.enabled", havingValue = "true")
        public static class JsonApiConfiguration {
            @Bean
            @RefreshScope
            @ConditionalOnMissingBean(name = "jsonApiController")
            public JsonApiController jsonApiController(RefreshableElide refreshableElide,
                    ElideConfigProperties settings, RouteResolver routeResolver) {
                return new JsonApiController(refreshableElide, settings, routeResolver);
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
                    ElideConfigProperties settings, OpenApiDocumentCustomizer customizer) {
                return buildApiDocsRegistrations(elide, settings, customizer);
            }

            @Bean
            @RefreshScope
            @ConditionalOnMissingBean(name = "apiDocsController")
            public ApiDocsController apiDocsController(ApiDocsController.ApiDocsRegistrations docs) {
                return new ApiDocsController(docs);
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
            public GraphqlController graphqlController(QueryRunners runners, JsonApiMapper jsonApiMapper,
                    HeaderUtils.HeaderProcessor headerProcessor, ElideConfigProperties settings,
                    RouteResolver routeResolver) {
                return new GraphqlController(runners, jsonApiMapper, headerProcessor, settings, routeResolver);
            }
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "spring.cloud.refresh.enabled", havingValue = "false", matchIfMissing = true)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public static class NonRefreshableConfiguration {
        /**
         * Creates the Elide instance with standard settings.
         * @param dictionary Stores the static metadata about Elide models.
         * @param dataStore The persistence store.
         * @param headerProcessor HTTP header function which is invoked for every request.
         * @param transactionRegistry Global transaction registry.
         * @param settings Elide settings.
         * @return A new elide instance.
         */
        @Bean
        @ConditionalOnMissingBean
        public RefreshableElide refreshableElide(EntityDictionary dictionary, DataStore dataStore,
                HeaderUtils.HeaderProcessor headerProcessor, TransactionRegistry transactionRegistry,
                ElideConfigProperties settings, JsonApiMapper mapper, ErrorMapper errorMapper) {
            return buildRefreshableElide(dictionary, dataStore, headerProcessor, transactionRegistry, settings, mapper,
                    errorMapper);
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.json-api.enabled", havingValue = "true")
        public static class JsonApiConfiguration {
            @Bean
            @ConditionalOnMissingBean(name = "jsonApiController")
            public JsonApiController jsonApiController(RefreshableElide refreshableElide,
                    ElideConfigProperties settings, RouteResolver routeResolver) {
                return new JsonApiController(refreshableElide, settings, routeResolver);
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
                    ElideConfigProperties settings, OpenApiDocumentCustomizer customizer) {
                return buildApiDocsRegistrations(elide, settings, customizer);
            }

            @Bean
            @ConditionalOnMissingBean(name = "apiDocsController")
            public ApiDocsController apiDocsController(ApiDocsController.ApiDocsRegistrations docs) {
                return new ApiDocsController(docs);
            }

            @Bean
            @ConditionalOnMissingBean
            public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
                return new BasicOpenApiDocumentCustomizer();
            }
        }

        @Configuration
        @ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
        public static class GraphQLConfiguration {
            @Bean
            @ConditionalOnMissingBean
            public QueryRunners queryRunners(RefreshableElide refreshableElide,
                    DataFetcherExceptionHandler exceptionHandler) {
                return new QueryRunners(refreshableElide, exceptionHandler);
            }

            @Bean
            @ConditionalOnMissingBean(name = "graphqlController")
            public GraphqlController graphqlController(QueryRunners runners, JsonApiMapper jsonApiMapper,
                    HeaderUtils.HeaderProcessor headerProcessor, ElideConfigProperties settings,
                    RouteResolver routeResolver) {
                return new GraphqlController(runners, jsonApiMapper, headerProcessor, settings, routeResolver);
            }
        }
    }

    public static RefreshableElide buildRefreshableElide(EntityDictionary dictionary, DataStore dataStore,
            HeaderUtils.HeaderProcessor headerProcessor, TransactionRegistry transactionRegistry,
            ElideConfigProperties settings, JsonApiMapper mapper, ErrorMapper errorMapper) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore).withEntityDictionary(dictionary)
                .withErrorMapper(errorMapper).withJsonApiMapper(mapper)
                .withDefaultMaxPageSize(settings.getMaxPageSize()).withDefaultPageSize(settings.getPageSize())
                .withJoinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .withSubqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .withAuditLogger(new Slf4jLogger()).withBaseUrl(settings.getBaseUrl())
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .withHeaderProcessor(headerProcessor);

        if (settings.isVerboseErrors()) {
            builder.withVerboseErrors();
        }

        if (settings.getAsync() != null && settings.getAsync().getExport() != null
                && settings.getAsync().getExport().isEnabled()) {
            builder.withExportApiPath(settings.getAsync().getExport().getPath());
        }

        if (settings.getGraphql() != null && settings.getGraphql().isEnabled()) {
            builder.withGraphQLApiPath(settings.getGraphql().getPath());

            if (settings.getGraphql().getFederation().isEnabled()) {
                builder.withGraphQLFederation(true);
            }
        }

        if (settings.getJsonApi() != null && settings.getJsonApi().isEnabled()) {
            builder.withJsonApiPath(settings.getJsonApi().getPath());

            if (settings.getJsonApi().getLinks().isEnabled()) {
                String baseUrl = settings.getBaseUrl();

                if (StringUtils.isEmpty(baseUrl)) {
                    builder.withJsonApiLinks(new DefaultJsonApiLinks());
                } else {
                    String jsonApiBaseUrl = baseUrl + settings.getJsonApi().getPath() + "/";
                    builder.withJsonApiLinks(new DefaultJsonApiLinks(jsonApiBaseUrl));
                }
            }
        }

        Elide elide = new Elide(builder.build(), transactionRegistry, dictionary.getScanner(), true);

        return new RefreshableElide(elide);
    }

    public static ApiDocsController.ApiDocsRegistrations buildApiDocsRegistrations(RefreshableElide elide,
            ElideConfigProperties settings, OpenApiDocumentCustomizer customizer) {
        String jsonApiPath = settings.getJsonApi() != null ? settings.getJsonApi().getPath() : null;

        EntityDictionary dictionary = elide.getElide().getElideSettings().getDictionary();

        List<ApiDocsRegistration> registrations = new ArrayList<>();
        dictionary.getApiVersions().stream().forEach(apiVersion -> {
            Supplier<OpenAPI> document = () -> {
                OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(apiVersion)
                        .supportLegacyFilterDialect(false);
                OpenAPI openApi = builder.build();
                openApi.addServersItem(new Server().url(jsonApiPath));
                customizer.customize(openApi);
                if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                    Info info = openApi.getInfo();
                    if (info == null) {
                        info = new Info();
                        openApi.setInfo(info);
                    }
                    info.setVersion(apiVersion);
                }
                return openApi;
            };
            registrations.add(new ApiDocsRegistration("", SingletonSupplier.of(document),
                    settings.getApiDocs().getVersion().getValue(), apiVersion));        });
        return new ApiDocsController.ApiDocsRegistrations(registrations);
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
