/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.Injector;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.contrib.swagger.SwaggerBuilder;
import com.yahoo.elide.contrib.swagger.resources.DocEndpoint;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.CaffeineCache;
import com.yahoo.elide.datastores.aggregation.core.NoopQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.security.checks.Check;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;

import io.swagger.models.Info;
import io.swagger.models.Swagger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Interface for configuring an ElideStandalone application.
 */
public interface ElideStandaloneSettings {
    /* Elide settings */

     public final Consumer<EntityManager> TXCANCEL = (em) -> { em.unwrap(Session.class).cancelQuery(); };

    /**
     * A map containing check mappings for security across Elide. If not provided, then an empty map is used.
     * In case of an empty map, checks can be referenced by their fully qualified class names.
     *
     * @return Check mappings.
     */
    default Map<String, Class<? extends Check>> getCheckMappings() {
        return Collections.emptyMap();
    }

    /**
     * Elide settings to be used for bootstrapping the Elide service. By default, this method constructs an
     * ElideSettings object using the application overrides provided in this class. If this method is overridden,
     * the returned settings object is used over any additional Elide setting overrides.
     *
     * That is to say, if you intend to override this method, expect to fully configure the ElideSettings object to
     * your needs.
     *
     * @param dictionary EntityDictionary object.
     * @param dataStore Dastore object
     * @return Configured ElideSettings object.
     */
    default ElideSettings getElideSettings(EntityDictionary dictionary, DataStore dataStore) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(getAuditLogger());

        if (enableISO8601Dates()) {
            builder = builder.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
        }

        return builder.build();
    }

    /* Non-required application/server settings */
    /**
     * Port for HTTP server to listen on.
     *
     * @return Default: 8080
     */
    default int getPort() {
        return 8080;
    }

    /**
     * Package name containing your models. This package will be recursively scanned for @Entity's and
     * registered with Elide.
     *
     * NOTE: This will scan for all entities in that package and bind this data to a set named "elideAllModels".
     *       If providing a custom ElideSettings object, you can inject this data into your class by using:
     *
     *       <strong>@Inject @Named("elideAllModels") Set&lt;Class&gt; entities;</strong>
     *
     * @return Default: com.yourcompany.elide.models
     */
    default String getModelPackageName() {
        return "com.yourcompany.elide.models";
    }

    /**
     * API root path specification for JSON-API. Namely, this is the mount point of your API.
     * By default it will look something like:
     *   <strong>yourcompany.com/api/v1/YOUR_ENTITY</strong>
     *
     * @return Default: /api/v1/*
     */
    default String getJsonApiPathSpec() {
        return "/api/v1/*";
    }

    /**
     * API root path specification for the GraphQL endpoint. Namely, this is the root uri for GraphQL.
     *
     * @return Default: /graphql/api/v1
     */
    default String getGraphQLApiPathSpec() {
        return "/graphql/api/v1/*";
    }

    /**
     * API root path specification for the Swagger endpoint. Namely, this is the root uri for Swagger docs.
     *
     * @return Default: /swagger/*
     */
    default String getSwaggerPathSpec() {
        return "/swagger/*";
    }

    /**
     * Enable the JSONAPI endpoint. If false, the endpoint will be disabled.
     *
     * @return Default: True
     */
    default boolean enableJSONAPI() {
        return true;
    }

    /**
     * Enable the GraphQL endpoint. If false, the endpoint will be disabled.
     *
     * @return Default: True
     */
    default boolean enableGraphQL() {
        return true;
    }

    /**
     * Enable the support for Dynamic Model Configuration. If false, the feature will be disabled.
     * If enabled, ensure that Aggregation Data Store is also  enabled
     * @return Default: False
     */
    default boolean enableDynamicModelConfig() {
        return false;
    }

    /**
     * Enable the support for Aggregation Data Store. If false, the feature will be disabled.
     *
     * @return Default: False
     */
    default boolean enableAggregationDataStore() {
        return false;
    }

    /**
     * Base path to Hjson dynamic model configurations.
     * @return Default: /models/
     */
    default String getDynamicConfigPath() {
        return File.separator + "models" + File.separator;
    }

    /**
     * Enable the support for Async querying feature. If false, the async feature will be disabled.
     *
     * @return Default: False
     */
    default boolean enableAsync() {
        return false;
    }

    /**
     * Enable the support for cleaning up Async query history. If false, the async cleanup feature will be disabled.
     *
     * @return Default: False
     */
    default boolean enableAsyncCleanup() {
        return false;
    }

    /**
     * Thread Size for Async queries to run in parallel.
     *
     * @return Default: 5
     */
    default Integer getAsyncThreadSize() {
        return 5;
    }

    /**
     * Maximum Query Run time for Async Queries to mark as TIMEDOUT.
     *
     * @return Default: 60
     */
    default Integer getAsyncMaxRunTimeSeconds() {
        return 60;
    }

    /**
     * Number of days history to retain for async query executions and results.
     *
     * @return Default: 7
     */
    default Integer getAsyncQueryCleanupDays() {
        return 7;
    }

    /**
     * Number of seconds  between async query cancellation checks.
     *
     * @return Default: 300
     */
    default Integer getAsyncQueryCancelCheckIntervalSeconds() {
        return 300;
    }

    /**
     * Implementation of AsyncQueryDAO to use.
     *
     * @return AsyncQueryDAO type object.
     */
    default AsyncQueryDAO getAsyncQueryDAO() {
        return null;
    }

    /**
     * Whether Dates should be ISO8601 strings (true) or epochs (false).
     * @return whether ISO8601Dates are enabled.
     */
    default boolean enableISO8601Dates() {
        return true;
    }

    /**
     * Whether or not Codahale metrics, healthchecks, thread, ping, and admin servlet
     * should be enabled.
     * @return  whether ServiceMonitoring is enabled.
     */
    default boolean enableServiceMonitoring() {
        return true;
    }

    /**
     * Enable swagger documentation.
     * @return whether Swagger is enabled;
     */
    default boolean enableSwagger() {
        return false;
    }

    /**
     * Swagger documentation requires an API version.
     * The models with the same version are included.
     * @return swagger version;
     */
    default String getSwaggerVersion() {
        return NO_VERSION;
    }

    /**
     * Swagger documentation requires an API name.
     * @return swagger service name;
     */
    default String getSwaggerName() {
        return "Elide Service";
    }

    /**
     * Creates a singular swagger document for JSON-API.
     * @param dictionary Contains the static metadata about Elide models. .
     * @return list of swagger registration objects.
     */
    default List<DocEndpoint.SwaggerRegistration> buildSwagger(EntityDictionary dictionary) {
        Info info = new Info()
                .title(getSwaggerName())
                .version(getSwaggerVersion());

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);

        String moduleBasePath = getJsonApiPathSpec().replaceAll("/\\*", "");

        Swagger swagger = builder.build().basePath(moduleBasePath);

        List<DocEndpoint.SwaggerRegistration> docs = new ArrayList<>();
        docs.add(new DocEndpoint.SwaggerRegistration("test", swagger));

        return docs;
    }

    /**
     * JAX-RS filters to register with the web service.
     *
     * @return Default: Empty
     */
    default List<Class<?>> getFilters() {
        return Collections.emptyList();
    }

    /**
     * Supplemental resource configuration for Elide application. This should be a fully qualified class name.
     *
     * Before calling the consumer, the class will be injected by the ServiceLocator.
     *
     * @return Default: null
     */
    default Consumer<ResourceConfig> getApplicationConfigurator() {
        // Do nothing by default
        return (x) -> { };
    }

    /**
     * Gets properties to configure the database.
     *
     * @return Default: ./settings/hibernate.cfg.xml
     */
    default Properties getDatabaseProperties() {
        return new Properties();
    }

    /**
     * A hook to directly modify the jetty servlet context handler as necessary.
     *
     * @param servletContextHandler ServletContextHandler in use by Elide standalone.
     */
    default void updateServletContextHandler(ServletContextHandler servletContextHandler) {
        // Do nothing
    }

    /**
     * Gets the audit logger for elide.
     *
     * @return Default: Slf4jLogger
     */
    default AuditLogger getAuditLogger() {
        return new Slf4jLogger();
    }

    /**
     * Limit on number of query cache entries. Non-positive values disable the query cache.
     *
     * @return Default: 1024
     */
    default Integer getQueryCacheMaximumEntries() {
        return CaffeineCache.DEFAULT_MAXIMUM_ENTRIES;
    }

    /**
     * Get the query cache implementation. If null, query cache is disabled.
     *
     * @return Default: {@code new CaffeineCache(getQueryCacheSize())}
     */
    default Cache getQueryCache() {
        return getQueryCacheMaximumEntries() > 0 ? new CaffeineCache(getQueryCacheMaximumEntries()) : null;
    }

    /**
     * Gets the dynamic compiler for elide.
     *
     * @return Optional ElideDynamicEntityCompiler
     */
    default Optional<ElideDynamicEntityCompiler> getDynamicCompiler() {
        ElideDynamicEntityCompiler dynamicEntityCompiler = null;

        if (enableAggregationDataStore() && enableDynamicModelConfig()) {
            try {
                dynamicEntityCompiler = new ElideDynamicEntityCompiler(getDynamicConfigPath());
            } catch (Exception e) { // thrown by in memory compiler
                throw new IllegalStateException(e);
            }
        }

        return Optional.ofNullable(dynamicEntityCompiler);
    }

    /**
     * Gets the DataStore for elide.
     * @param metaDataStore MetaDataStore object.
     * @param aggregationDataStore AggregationDataStore object.
     * @param entityManagerFactory EntityManagerFactory object.
     * @return DataStore object initialized.
     */
    default DataStore getDataStore(MetaDataStore metaDataStore, AggregationDataStore aggregationDataStore,
            EntityManagerFactory entityManagerFactory) {
        DataStore jpaDataStore = new JpaDataStore(
                () -> { return entityManagerFactory.createEntityManager(); },
                (em) -> { return new NonJtaTransaction(em, TXCANCEL); });

        DataStore dataStore = new MultiplexManager(jpaDataStore, metaDataStore, aggregationDataStore);

        return dataStore;
    }

    /**
     * Gets the DataStore for elide when aggregation store is disabled.
     * @param entityManagerFactory EntityManagerFactory object.
     * @return DataStore object initialized.
     */
    default DataStore getDataStore(EntityManagerFactory entityManagerFactory) {
        DataStore jpaDataStore = new JpaDataStore(
                () -> { return entityManagerFactory.createEntityManager(); },
                (em) -> { return new NonJtaTransaction(em, TXCANCEL); });

        return jpaDataStore;
    }

    /**
     * Gets the AggregationDataStore for elide.
     * @param queryEngine query engine object.
     * @param optionalCompiler optional dynamic compiler object.
     * @return AggregationDataStore object initialized.
     */
    default AggregationDataStore getAggregationDataStore(QueryEngine queryEngine,
            Optional<ElideDynamicEntityCompiler> optionalCompiler) {
        AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = AggregationDataStore.builder()
                .queryEngine(queryEngine).queryLogger(new NoopQueryLogger());

        if (enableDynamicModelConfig()) {
            Set<Class<?>> annotatedClasses = getDynamicClassesIfAvailable(optionalCompiler, FromTable.class);
            annotatedClasses.addAll(getDynamicClassesIfAvailable(optionalCompiler, FromSubquery.class));
            aggregationDataStoreBuilder.dynamicCompiledClasses(annotatedClasses);
        }
        aggregationDataStoreBuilder.cache(getQueryCache());
        return aggregationDataStoreBuilder.build();
    }

    /**
     * Gets the EntityDictionary for elide.
     * @param injector Service locator for web service for dependency injection.
     * @param optionalCompiler optional dynamic compiler object.
     * @return EntityDictionary object initialized.
     */
    default EntityDictionary getEntityDictionary(ServiceLocator injector,
            Optional<ElideDynamicEntityCompiler> optionalCompiler) {
        EntityDictionary dictionary = new EntityDictionary(getCheckMappings(),
                new Injector() {
                    @Override
                    public void inject(Object entity) {
                        injector.inject(entity);
                    }

                    @Override
                    public <T> T instantiate(Class<T> cls) {
                        return injector.create(cls);
                    }
                });

        dictionary.scanForSecurityChecks();

        Set<Class<?>> annotatedSecurityClasses = getDynamicClassesIfAvailable(optionalCompiler, SecurityCheck.class);

        dictionary.addSecurityChecks(annotatedSecurityClasses);

        return dictionary;
    }

    /**
     * Gets the metadatastore for elide.
     * @param optionalCompiler optional dynamic compiler object.
     * @return MetaDataStore object initialized.
     */
    default MetaDataStore getMetaDataStore(Optional<ElideDynamicEntityCompiler> optionalCompiler) {
        MetaDataStore metaDataStore = null;

        if (optionalCompiler.isPresent()) {
            try {
                metaDataStore = new MetaDataStore(optionalCompiler.get());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        } else {
            metaDataStore = new MetaDataStore();
        }

        return metaDataStore;
    }

    /**
     * Gets the QueryEngine for elide.
     * @param metaDataStore MetaDataStore object.
     * @param entityManagerFactory EntityManagerFactory object.
     * @return QueryEngine object initialized.
     */
    default QueryEngine getQueryEngine(MetaDataStore metaDataStore, EntityManagerFactory entityManagerFactory) {
        return new SQLQueryEngine(metaDataStore, entityManagerFactory, TXCANCEL);
    }

    static Set<Class<?>> getDynamicClassesIfAvailable(Optional<ElideDynamicEntityCompiler> optionalCompiler,
            Class<?> classz) {
        Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();

        if (!optionalCompiler.isPresent()) {
            return annotatedClasses;
        }

        ElideDynamicEntityCompiler compiler = optionalCompiler.get();

        try {
            annotatedClasses = compiler.findAnnotatedClasses(classz);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        return annotatedClasses;
    }
}
