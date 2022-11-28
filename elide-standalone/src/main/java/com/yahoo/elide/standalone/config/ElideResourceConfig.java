/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.export.formatter.CSVExportFormatter;
import com.yahoo.elide.async.export.formatter.JSONExportFormatter;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.hooks.AsyncQueryHook;
import com.yahoo.elide.async.hooks.TableExportHook;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.resources.ExportApiEndpoint.ExportApiProperties;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.modelconfig.DynamicConfiguration;
import com.yahoo.elide.standalone.Util;
import com.yahoo.elide.swagger.resources.DocEndpoint;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.inject.Inject;
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
    public static final String ASYNC_EXECUTOR_ATTR = "asyncExecutor";
    public static final String ASYNC_UPDATER_ATTR = "asyncUpdater";

    private static MetricRegistry metricRegistry = null;
    private static HealthCheckRegistry healthCheckRegistry = null;

    @AllArgsConstructor
    public class ElideBinder extends AbstractBinder {

        private ClassScanner classScanner;
        private Optional<DynamicConfiguration> dynamicConfiguration;
        private ServletContext servletContext;

        @Override
        protected void configure() {
            ElideStandaloneAsyncSettings asyncProperties = settings.getAsyncProperties() == null
                    ? new ElideStandaloneAsyncSettings() { } : settings.getAsyncProperties();
            EntityManagerFactory entityManagerFactory = Util.getEntityManagerFactory(classScanner,
                    settings.getModelPackageName(), asyncProperties.enabled(), settings.getDatabaseProperties());


            EntityDictionary dictionary = settings.getEntityDictionary(injector, classScanner, dynamicConfiguration,
                    settings.getEntitiesToExclude());

            DataStore dataStore;

            if (settings.getAnalyticProperties().enableAggregationDataStore()) {
                MetaDataStore metaDataStore = settings.getMetaDataStore(classScanner, dynamicConfiguration);
                if (metaDataStore == null) {
                    throw new IllegalStateException("Aggregation Datastore is enabled but metaDataStore is null");
                }

                DataSource defaultDataSource = Util.getDataSource(settings.getDatabaseProperties());
                ConnectionDetails defaultConnectionDetails = new ConnectionDetails(defaultDataSource,
                        SQLDialectFactory.getDialect(settings.getAnalyticProperties().getDefaultDialect()));

                QueryEngine queryEngine = settings.getQueryEngine(metaDataStore, defaultConnectionDetails,
                        dynamicConfiguration, settings.getDataSourceConfiguration(),
                        settings.getAnalyticProperties().getDBPasswordExtractor());
                AggregationDataStore aggregationDataStore = settings.getAggregationDataStore(queryEngine);
                if (aggregationDataStore == null) {
                    throw new IllegalStateException(
                            "Aggregation Datastore is enabled but aggregationDataStore is null");
                }
                dataStore = settings.getDataStore(metaDataStore, aggregationDataStore, entityManagerFactory);

            } else {
                dataStore = settings.getDataStore(entityManagerFactory);
            }

            ElideSettings elideSettings = settings.getElideSettings(dictionary, dataStore,
                    settings.getObjectMapper());
            Elide elide = new Elide(elideSettings, new TransactionRegistry(),
                    elideSettings.getDictionary().getScanner(), false);

            // Bind elide instance for injection into endpoint
            bind(elide).to(Elide.class).named("elide");

            // Bind additional elements
            bind(elideSettings).to(ElideSettings.class);
            bind(elideSettings.getDictionary()).to(EntityDictionary.class);
            bind(elideSettings.getDataStore()).to(DataStore.class).named("elideDataStore");

            // Binding async service
            if (asyncProperties.enabled()) {
                bindAsync(asyncProperties, elide, dictionary);
            }
        }

        protected void bindAsync(
                ElideStandaloneAsyncSettings asyncProperties,
                Elide elide,
                EntityDictionary dictionary
        ) {
            AsyncAPIDAO asyncAPIDao = asyncProperties.getAPIDAO();
            if (asyncAPIDao == null) {
                asyncAPIDao = new DefaultAsyncAPIDAO(elide.getElideSettings(), elide.getDataStore());
            }
            bind(asyncAPIDao).to(AsyncAPIDAO.class);

            ExecutorService executor = (ExecutorService) servletContext.getAttribute(ASYNC_EXECUTOR_ATTR);
            ExecutorService updater = (ExecutorService) servletContext.getAttribute(ASYNC_UPDATER_ATTR);
            AsyncExecutorService asyncExecutorService =
                    new AsyncExecutorService(elide, executor, updater, asyncAPIDao);
            bind(asyncExecutorService).to(AsyncExecutorService.class);

            if (asyncProperties.enableExport()) {
                ExportApiProperties exportApiProperties = new ExportApiProperties(
                        asyncProperties.getExportAsyncResponseExecutor(),
                        asyncProperties.getExportAsyncResponseTimeoutSeconds());
                bind(exportApiProperties).to(ExportApiProperties.class).named("exportApiProperties");

                ResultStorageEngine resultStorageEngine = asyncProperties.getResultStorageEngine();
                if (resultStorageEngine == null) {
                    resultStorageEngine = new FileResultStorageEngine(asyncProperties.getStorageDestination(),
                            asyncProperties.enableExtension());
                }
                bind(resultStorageEngine).to(ResultStorageEngine.class).named("resultStorageEngine");

                // Initialize the Formatters.
                Map<ResultType, TableExportFormatter> supportedFormatters = new HashMap<>();
                supportedFormatters.put(ResultType.CSV, new CSVExportFormatter(elide,
                        asyncProperties.skipCSVHeader()));
                supportedFormatters.put(ResultType.JSON, new JSONExportFormatter(elide));

                // Binding TableExport LifeCycleHook
                TableExportHook tableExportHook = getTableExportHook(asyncExecutorService,
                        asyncProperties, supportedFormatters, resultStorageEngine);
                dictionary.bindTrigger(TableExport.class, CREATE, PREFLUSH, tableExportHook, false);
                dictionary.bindTrigger(TableExport.class, CREATE, POSTCOMMIT, tableExportHook, false);
                dictionary.bindTrigger(TableExport.class, CREATE, PRESECURITY, tableExportHook, false);
            }

            // Binding AsyncQuery LifeCycleHook
            AsyncQueryHook asyncQueryHook = new AsyncQueryHook(asyncExecutorService,
                    asyncProperties.getMaxAsyncAfterSeconds());

            dictionary.bindTrigger(AsyncQuery.class, CREATE, PREFLUSH, asyncQueryHook, false);
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

        ClassScanner classScanner = settings.getClassScanner();

        // Bind things that should be injectable to the Settings class
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                ElideStandaloneAsyncSettings asyncProperties = settings.getAsyncProperties();
                bind(Util.combineModelEntities(
                        classScanner,
                        settings.getModelPackageName(),
                        asyncProperties.enabled())).to(Set.class).named("elideAllModels");
            }
        });

        Optional<DynamicConfiguration> dynamicConfiguration;
        try {
            dynamicConfiguration = settings.getDynamicConfiguration(classScanner);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }


        // Bind to injector
        register(new ElideBinder(classScanner, dynamicConfiguration, servletContext));

        // Bind swaggers to given endpoint
        //This looks strange, but Jersey binds its Abstract binders first, and then later it binds 'external'
        //binders (like this HK2 version).  This allows breaking dependency injection into two phases.
        //Everything bound in the first phase can be accessed in the second phase.
        register(new org.glassfish.hk2.utilities.binding.AbstractBinder() {
            @Override
            protected void configure() {
                Elide elide = injector.getService(Elide.class, "elide");

                elide.doScans();

                //Bind subscription hooks.
                if (settings.getSubscriptionProperties().publishingEnabled()) {
                    settings.getSubscriptionProperties().subscriptionScanner(elide,
                            settings.getSubscriptionProperties().getConnectionFactory());
                }

                EntityDictionary dictionary = elide.getElideSettings().getDictionary();

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

    // TODO Remove this method when ElideSettings has all the settings.
    // Then the check can be done in TableExportHook.
    // Trying to avoid adding too many individual properties to ElideSettings for now.
    // https://github.com/yahoo/elide/issues/1803
    private TableExportHook getTableExportHook(AsyncExecutorService asyncExecutorService,
            ElideStandaloneAsyncSettings asyncProperties, Map<ResultType, TableExportFormatter> supportedFormatters,
            ResultStorageEngine engine) {
        TableExportHook tableExportHook = null;
        if (asyncProperties.enableExport()) {
            tableExportHook = new TableExportHook(asyncExecutorService, asyncProperties.getMaxAsyncAfterSeconds(),
                    supportedFormatters, engine);
        } else {
            tableExportHook = new TableExportHook(asyncExecutorService, asyncProperties.getMaxAsyncAfterSeconds(),
                    supportedFormatters, engine) {
                @Override
                public void validateOptions(AsyncAPI export, RequestScope requestScope) {
                    throw new InvalidOperationException("TableExport is not supported.");
                }
            };
        }
        return tableExportHook;
    }
}
