/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.standalone.config;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.ResultTypeFileExtensionMapper;
import com.paiondata.elide.async.export.formatter.TableExportFormatter;
import com.paiondata.elide.async.hooks.AsyncQueryHook;
import com.paiondata.elide.async.hooks.TableExportHook;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.resources.ExportApiEndpoint.ExportApiProperties;
import com.paiondata.elide.async.service.AsyncCleanerService;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.async.service.dao.DefaultAsyncApiDao;
import com.paiondata.elide.async.service.storageengine.FileResultStorageEngine;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.datastores.aggregation.AggregationDataStore;
import com.paiondata.elide.datastores.aggregation.QueryEngine;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.paiondata.elide.modelconfig.DynamicConfiguration;
import com.paiondata.elide.standalone.Util;
import com.paiondata.elide.swagger.resources.ApiDocsEndpoint;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import graphql.execution.DataFetcherExceptionHandler;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Context;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.sql.DataSource;

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
                    settings.getJsonApiMapper());
            Elide elide = new Elide(elideSettings, new TransactionRegistry(),
                    elideSettings.getEntityDictionary().getScanner(), false);

            // Bind elide instance for injection into endpoint
            bind(elide).to(Elide.class).named("elide");

            // Bind additional elements
            bind(elideSettings).to(ElideSettings.class);
            bind(elideSettings.getEntityDictionary()).to(EntityDictionary.class);
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
            AsyncApiDao asyncApiDao = asyncProperties.getAsyncApiDao();
            if (asyncApiDao == null) {
                asyncApiDao = new DefaultAsyncApiDao(elide.getElideSettings(), elide.getDataStore());
            }
            bind(asyncApiDao).to(AsyncApiDao.class);

            ExecutorService executor = (ExecutorService) servletContext.getAttribute(ASYNC_EXECUTOR_ATTR);
            ExecutorService updater = (ExecutorService) servletContext.getAttribute(ASYNC_UPDATER_ATTR);
            AsyncExecutorService asyncExecutorService = new AsyncExecutorService(elide, executor, updater, asyncApiDao,
                    Optional.of(settings.getDataFetcherExceptionHandler()));
            bind(asyncExecutorService).to(AsyncExecutorService.class);

            if (asyncProperties.enableExport()) {
                ExportApiProperties exportApiProperties = new ExportApiProperties(
                        asyncProperties.getExportAsyncResponseExecutor(),
                        asyncProperties.getExportAsyncResponseTimeout());
                bind(exportApiProperties).to(ExportApiProperties.class).named("exportApiProperties");

                ResultStorageEngine resultStorageEngine = asyncProperties.getResultStorageEngine();
                if (resultStorageEngine == null) {
                    resultStorageEngine = new FileResultStorageEngine(asyncProperties.getStorageDestination());
                }
                bind(resultStorageEngine).to(ResultStorageEngine.class).named("resultStorageEngine");

                // Binding TableExport LifeCycleHook
                TableExportHook tableExportHook = getTableExportHook(asyncExecutorService,
                        asyncProperties, asyncProperties.getTableExportFormattersBuilder(elide).build(),
                        resultStorageEngine,
                        asyncProperties.appendFileExtension() ? asyncProperties.getResultTypeFileExtensionMapper()
                                : null);
                dictionary.bindTrigger(TableExport.class, CREATE, PREFLUSH, tableExportHook, false);
                dictionary.bindTrigger(TableExport.class, CREATE, POSTCOMMIT, tableExportHook, false);
                dictionary.bindTrigger(TableExport.class, CREATE, PRESECURITY, tableExportHook, false);
            }

            // Binding AsyncQuery LifeCycleHook
            AsyncQueryHook asyncQueryHook = new AsyncQueryHook(asyncExecutorService,
                    asyncProperties.getMaxAsyncAfter());

            dictionary.bindTrigger(AsyncQuery.class, CREATE, PREFLUSH, asyncQueryHook, false);
            dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, asyncQueryHook, false);
            dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, asyncQueryHook, false);

            // Binding async cleanup service
            if (asyncProperties.enableCleanup()) {
                AsyncCleanerService.init(elide, asyncProperties.getQueryMaxRunTime(),
                        asyncProperties.getQueryRetentionDuration(),
                        asyncProperties.getQueryCancellationCheckInterval(), asyncApiDao);
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
                bind(settings.getDataFetcherExceptionHandler()).to(DataFetcherExceptionHandler.class)
                        .named("dataFetcherExceptionHandler");
                if (settings.getRouteResolver() != null) {
                    bind(settings.getRouteResolver()).to(RouteResolver.class).named("routeResolver");
                }
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

        // Bind api docs to given endpoint
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

                EntityDictionary dictionary = elide.getElideSettings().getEntityDictionary();

                if (settings.enableApiDocs()) {
                    List<ApiDocsEndpoint.ApiDocsRegistration> apiDocs = settings.buildApiDocs(dictionary);
                    bind(apiDocs).named("apiDocs").to(new TypeLiteral<List<ApiDocsEndpoint.ApiDocsRegistration>>() {
                    });
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

    private TableExportHook getTableExportHook(AsyncExecutorService asyncExecutorService,
            ElideStandaloneAsyncSettings asyncProperties, Map<String, TableExportFormatter> supportedFormatters,
            ResultStorageEngine engine, ResultTypeFileExtensionMapper resultTypeFileExtensionMapper) {
        return new TableExportHook(asyncExecutorService, asyncProperties.getMaxAsyncAfter(),
                    supportedFormatters, engine, resultTypeFileExtensionMapper);
    }
}
