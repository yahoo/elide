/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.READ;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.hooks.AsyncQueryHook;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.modelconfig.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.standalone.Util;
import com.yahoo.elide.swagger.resources.DocEndpoint;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.core.Context;

/**
 * Elide application resource configuration file.
 */
@Slf4j
public class ElideResourceConfig extends ResourceConfig {
    private final ElideStandaloneSettings settings;
    private final ServiceLocator injector;

    public static final String ELIDE_STANDALONE_SETTINGS_ATTR = "elideStandaloneSettings";

    private static MetricRegistry metricRegistry = null;
    private static HealthCheckRegistry healthCheckRegistry = null;

    /**
     * Constructor.
     *
     * @param injector Injection instance for application.
     * @param servletContext servlet context instance.
     */
    @Inject
    public ElideResourceConfig(ServiceLocator injector, @Context ServletContext servletContext) {
        this.injector = injector;

        settings = (ElideStandaloneSettings) servletContext.getAttribute(ELIDE_STANDALONE_SETTINGS_ATTR);

        Optional<ElideDynamicEntityCompiler> optionalCompiler = settings.getDynamicCompiler();

        // Bind things that should be injectable to the Settings class
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                ElideStandaloneAsyncSettings asyncProperties = settings.getAsyncProperties();
                bind(Util.combineModelEntities(optionalCompiler, settings.getModelPackageName(),
                        asyncProperties.enabled())).to(Set.class).named("elideAllModels");
            }
        });

        // Bind to injector
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                ElideStandaloneAsyncSettings asyncProperties = settings.getAsyncProperties();
                EntityManagerFactory entityManagerFactory = Util.getEntityManagerFactory(settings.getModelPackageName(),
                        asyncProperties.enabled(), optionalCompiler, settings.getDatabaseProperties());

                EntityDictionary dictionary = settings.getEntityDictionary(injector, optionalCompiler);

                DataStore dataStore;

                if (settings.getAnalyticProperties().enableAggregationDataStore()) {
                    MetaDataStore metaDataStore = settings.getMetaDataStore(optionalCompiler);
                    if (metaDataStore == null) {
                        throw new IllegalStateException("Aggregation Datastore is enabled but metaDataStore is null");
                    }

                    DataSource defaultDataSource = Util.getDataSource(settings.getDatabaseProperties());
                    ConnectionDetails defaultConnectionDetails = new ConnectionDetails(defaultDataSource,
                                    SQLDialectFactory.getDialect(settings.getAnalyticProperties().getDefaultDialect()));

                    QueryEngine queryEngine = settings.getQueryEngine(metaDataStore, defaultConnectionDetails,
                                    optionalCompiler, settings.getDataSourceConfiguration(),
                                    settings.getAnalyticProperties().getDBPasswordExtractor());
                    AggregationDataStore aggregationDataStore =
                                    settings.getAggregationDataStore(queryEngine, optionalCompiler);
                    if (aggregationDataStore == null) {
                        throw new IllegalStateException(
                                        "Aggregation Datastore is enabled but aggregationDataStore is null");
                    }
                    dataStore = settings.getDataStore(metaDataStore, aggregationDataStore, entityManagerFactory);
                } else {
                    dataStore = settings.getDataStore(entityManagerFactory);
                }

                ElideSettings elideSettings = settings.getElideSettings(dictionary, dataStore);

                Elide elide = new Elide(elideSettings);

                // Bind elide instance for injection into endpoint
                bind(elide).to(Elide.class).named("elide");

                // Bind additional elements
                bind(elideSettings).to(ElideSettings.class);
                bind(elideSettings.getDictionary()).to(EntityDictionary.class);
                bind(elideSettings.getDataStore()).to(DataStore.class).named("elideDataStore");

                // Binding async service
                if (asyncProperties.enabled()) {

                    AsyncAPIDAO asyncAPIDao = asyncProperties.getAPIDAO();
                    if (asyncAPIDao == null) {
                        asyncAPIDao = new DefaultAsyncAPIDAO(elide.getElideSettings(), elide.getDataStore());
                    }
                    bind(asyncAPIDao).to(AsyncAPIDAO.class);

                    // TODO: If null, initialize with FileResultStorageEngine
                    ResultStorageEngine resultStorageEngine = asyncProperties.getResultStorageEngine();
                    AsyncExecutorService.init(elide, asyncProperties.getThreadSize(), asyncAPIDao,
                            resultStorageEngine, settings.enableGraphQL());
                    log.info("reousrce setting --> settings.enableGraphQL() --> " + settings.enableGraphQL());
                    bind(AsyncExecutorService.getInstance()).to(AsyncExecutorService.class);

                    // Binding AsyncQuery LifeCycleHook
                    AsyncQueryHook asyncQueryHook = new AsyncQueryHook(AsyncExecutorService.getInstance(),
                            asyncProperties.getMaxAsyncAfterSeconds());

                    dictionary.bindTrigger(AsyncQuery.class, READ, PRESECURITY, asyncQueryHook, false);
                    dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, asyncQueryHook, false);
                    dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, asyncQueryHook, false);

                    // Binding async cleanup service
                    if (asyncProperties.enableCleanup()) {
                        AsyncCleanerService.init(elide, asyncProperties.getMaxRunTimeSeconds(),
                                asyncProperties.getQueryCleanupDays(),
                                asyncProperties.getQueryCancelCheckIntervalSeconds(), asyncAPIDao);
                        bind(AsyncCleanerService.getInstance()).to(AsyncCleanerService.class);
                    }
                }
            }
        });

        // Bind swaggers to given endpoint
        register(new org.glassfish.hk2.utilities.binding.AbstractBinder() {
            @Override
            protected void configure() {
                EntityDictionary dictionary = injector.getService(EntityDictionary.class);

                if (settings.enableSwagger()) {

                    List<DocEndpoint.SwaggerRegistration> swaggerDocs = settings.buildSwagger(dictionary);

                    bind(swaggerDocs).named("swagger").to(new TypeLiteral<List<DocEndpoint.SwaggerRegistration>>() { });
                }
            }
        });

        registerFilters(settings.getFilters());

        additionalConfiguration(settings.getApplicationConfigurator());
    }

    /**
     * Init the supplemental resource config.
     */
    private void additionalConfiguration(Consumer<ResourceConfig> configurator) {
        // Inject into consumer if class is provided
        injector.inject(configurator);
        configurator.accept(this);
    }

    /**
     * Register provided JAX-RS filters.
     */
    private void registerFilters(List<Class<?>> filters) {
        filters.forEach(this::register);
    }

    public static MetricRegistry getMetricRegistry() {
        if (metricRegistry == null) {
            metricRegistry = new MetricRegistry();
        }

        return metricRegistry;
    }

    public static HealthCheckRegistry getHealthCheckRegistry() {
        if (healthCheckRegistry == null) {
            healthCheckRegistry = new HealthCheckRegistry();
        }

        return healthCheckRegistry;
    }
}
