/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.CaffeineCache;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.DataSourceConfiguration;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.AggregateBeforeJoinOptimizer;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.jsonapi.links.DefaultJSONApiLinks;
import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import com.yahoo.elide.modelconfig.DynamicConfiguration;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import com.yahoo.elide.swagger.SwaggerBuilder;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Auto Configuration For Elide Services.  Override any of the beans (by defining your own) to change
 * the default behavior.
 */
@Configuration
@EnableConfigurationProperties(ElideConfigProperties.class)
@Slf4j
public class ElideAutoConfiguration {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private final Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();

    /**
     * Creates dynamic configuration for models, security roles, and database connections.
     * @param settings Config Settings.
     * @throws IOException if there is an error reading the configuration.
     * @return An instance of DynamicConfiguration.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${elide.aggregation-store.enabled:false} and ${elide.dynamic-config.enabled:false}")
    public DynamicConfiguration buildDynamicConfiguration(ElideConfigProperties settings) throws IOException {
        DynamicConfigValidator validator = new DynamicConfigValidator(settings.getDynamicConfig().getPath());
        validator.readAndValidateConfigs();
        return validator;
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

    /**
     * Creates the Elide instance with standard settings.
     * @param dictionary Stores the static metadata about Elide models.
     * @param dataStore The persistence store.
     * @param settings Elide settings.
     * @return A new elide instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public Elide initializeElide(EntityDictionary dictionary,
            DataStore dataStore, ElideConfigProperties settings) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withDefaultMaxPageSize(settings.getMaxPageSize())
                .withDefaultPageSize(settings.getPageSize())
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(new Slf4jLogger())
                .withBaseUrl(settings.getBaseUrl())
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .withJsonApiPath(settings.getJsonApi().getPath())
                .withGraphQLApiPath(settings.getGraphql().getPath());

        if (settings.isVerboseErrors()) {
            builder.withVerboseErrors();
        }

        if (settings.getAsync() != null
                && settings.getAsync().getExport() != null
                && settings.getAsync().getExport().isEnabled()) {
            builder.withExportApiPath(settings.getAsync().getExport().getPath());
        }

        if (settings.getJsonApi() != null
                && settings.getJsonApi().isEnabled()
                && settings.getJsonApi().isEnableLinks()) {
            String baseUrl = settings.getBaseUrl();

            if (StringUtils.isEmpty(baseUrl)) {
                builder.withJSONApiLinks(new DefaultJSONApiLinks());
            } else {
                String jsonApiBaseUrl = baseUrl + settings.getJsonApi().getPath() + "/";
                builder.withJSONApiLinks(new DefaultJSONApiLinks(jsonApiBaseUrl));
            }
        }

        return new Elide(builder.build());
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
     * Creates the entity dictionary for Elide which contains static metadata about Elide models.
     * Override to load check classes or life cycle hooks.
     * @param beanFactory Injector to inject Elide models.
     * @param dynamicConfig An instance of DynamicConfiguration.
     * @param settings Elide configuration settings.
     * @param entitiesToExclude set of Entities to exclude from binding.
     * @return a newly configured EntityDictionary.
     * @throws ClassNotFoundException Exception thrown.
     */
    @Bean
    @ConditionalOnMissingBean
    public EntityDictionary buildDictionary(AutowireCapableBeanFactory beanFactory,
                                            @Autowired(required = false) DynamicConfiguration dynamicConfig,
                                            ElideConfigProperties settings,
                                            @Qualifier("entitiesToExclude") Set<Type<?>> entitiesToExclude)
            throws ClassNotFoundException {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>(),
                new Injector() {
                    @Override
                    public void inject(Object entity) {
                        beanFactory.autowireBean(entity);
                    }

                    @Override
                    public <T> T instantiate(Class<T> cls) {
                        return beanFactory.createBean(cls);
                    }
                }, entitiesToExclude);

        dictionary.scanForSecurityChecks();

        if (isAggregationStoreEnabled(settings) && isDynamicConfigEnabled(settings)) {
            dynamicConfig.getRoles().forEach(role -> {
                dictionary.addRoleCheck(role, new Role.RoleMemberCheck(role));
            });
        }

        return dictionary;
    }

    /**
     * Create a QueryEngine instance for aggregation data store to use.
     * @param defaultDataSource DataSource for JPA.
     * @param dynamicConfig An instance of DynamicConfiguration.
     * @param settings Elide configuration settings.
     * @param dataSourceConfiguration DataSource Configuration
     * @param dbPasswordExtractor Password Extractor Implementation
     * @return An instance of a QueryEngine
     * @throws ClassNotFoundException Exception thrown.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    public QueryEngine buildQueryEngine(DataSource defaultDataSource,
                                        @Autowired(required = false) DynamicConfiguration dynamicConfig,
                                        ElideConfigProperties settings,
                                        DataSourceConfiguration dataSourceConfiguration,
                                        DBPasswordExtractor dbPasswordExtractor) throws ClassNotFoundException {

        boolean enableMetaDataStore = settings.getAggregationStore().isEnableMetaDataStore();
        ConnectionDetails defaultConnectionDetails = new ConnectionDetails(defaultDataSource,
                        SQLDialectFactory.getDialect(settings.getAggregationStore().getDefaultDialect()));
        if (isDynamicConfigEnabled(settings)) {
            MetaDataStore metaDataStore = new MetaDataStore(dynamicConfig.getTables(),
                    dynamicConfig.getNamespaceConfigurations(), enableMetaDataStore);
            Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();

            dynamicConfig.getDatabaseConfigurations().forEach(dbConfig -> {
                connectionDetailsMap.put(dbConfig.getName(),
                                new ConnectionDetails(
                                                dataSourceConfiguration.getDataSource(dbConfig, dbPasswordExtractor),
                                                SQLDialectFactory.getDialect(dbConfig.getDialect())));
            });

            return new SQLQueryEngine(metaDataStore, defaultConnectionDetails, connectionDetailsMap,
                    new HashSet<>(Arrays.asList(new AggregateBeforeJoinOptimizer(metaDataStore))),
                    new DefaultQueryValidator(metaDataStore.getMetadataDictionary()));
        }
        MetaDataStore metaDataStore = new MetaDataStore(enableMetaDataStore);
        return new SQLQueryEngine(metaDataStore, defaultConnectionDetails);
    }

    /**
     * Creates the DataStore Elide.  Override to use a different store.
     * @param entityManagerFactory The JPA factory which creates entity managers.
     * @param queryEngine QueryEngine instance for aggregation data store.
     * @param settings Elide configuration settings.
     * @return An instance of a JPA DataStore.
     * @throws ClassNotFoundException Exception thrown.
     */
    @Bean
    @ConditionalOnMissingBean
    public DataStore buildDataStore(EntityManagerFactory entityManagerFactory,
                                    @Autowired(required = false) QueryEngine queryEngine,
                                    ElideConfigProperties settings,
                                    @Autowired(required = false) Cache cache,
                                    @Autowired(required = false) QueryLogger querylogger)
            throws ClassNotFoundException {

        JpaDataStore jpaDataStore = new JpaDataStore(
                entityManagerFactory::createEntityManager,
                em -> new NonJtaTransaction(em, txCancel,
                        DEFAULT_LOGGER,
                        settings.getJpaStore().isDelegateToInMemoryStore()));

        if (isAggregationStoreEnabled(settings)) {
            AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder =
                            AggregationDataStore.builder().queryEngine(queryEngine);
            if (isDynamicConfigEnabled(settings)) {
                aggregationDataStoreBuilder.dynamicCompiledClasses(queryEngine.getMetaDataStore().getDynamicTypes());
            }
            aggregationDataStoreBuilder.cache(cache);
            aggregationDataStoreBuilder.queryLogger(querylogger);
            AggregationDataStore aggregationDataStore = aggregationDataStoreBuilder.build();

            // meta data store needs to be put at first to populate meta data models
            return new MultiplexManager(jpaDataStore, queryEngine.getMetaDataStore(), aggregationDataStore);
        }

        return jpaDataStore;
    }

    /**
     * Creates a query result cache to be used by {@link #buildDataStore}, or null if cache is to be disabled.
     * @param settings Elide configuration settings.
     * @return An instance of a query cache, or null.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    public Cache buildQueryCache(ElideConfigProperties settings) {
        CaffeineCache cache = null;

        int maxCacheItems = settings.getAggregationStore().getQueryCacheMaximumEntries();
        if (maxCacheItems > 0) {
            cache = new CaffeineCache(maxCacheItems, settings.getAggregationStore().getDefaultCacheExpirationMinutes());
            if (meterRegistry != null) {
                CaffeineCacheMetrics.monitor(meterRegistry, cache.getImplementation(), "elideQueryCache");
            }
        }
        return cache;
    }

    /**
     * Creates a querylogger to be used by {@link #buildDataStore} for aggregation.
     * @return The default Noop QueryLogger.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.aggregation-store.enabled", havingValue = "true")
    public QueryLogger buildQueryLogger() {
        return new Slf4jQueryLogger();
    }

    /**
     * Creates a singular swagger document for JSON-API.
     * @param dictionary Contains the static metadata about Elide models.
     * @param settings Elide configuration settings.
     * @return An instance of a JPA DataStore.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.swagger.enabled", havingValue = "true")
    public Swagger buildSwagger(EntityDictionary dictionary, ElideConfigProperties settings) {
        Info info = new Info()
                .title(settings.getSwagger().getName())
                .version(settings.getSwagger().getVersion());

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info).withLegacyFilterDialect(false);

        return builder.build().basePath(settings.getJsonApi().getPath());
    }

    private boolean isDynamicConfigEnabled(ElideConfigProperties settings) {

        boolean enabled = false;
        if (settings.getDynamicConfig() != null) {
            enabled = settings.getDynamicConfig().isEnabled();
        }

        return enabled;

    }

    private boolean isAggregationStoreEnabled(ElideConfigProperties settings) {

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
