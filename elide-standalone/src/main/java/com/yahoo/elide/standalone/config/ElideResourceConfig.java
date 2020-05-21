/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.hooks.ExecuteQueryHook;
import com.yahoo.elide.async.hooks.UpdatePrincipalNameHook;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.async.service.DefaultAsyncQueryDAO;
import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.contrib.swagger.resources.DocEndpoint;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.standalone.Util;

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
                bind(Util.combineModelEntities(optionalCompiler, settings.getModelPackageName(),
                        settings.enableAsync())).to(Set.class).named("elideAllModels");
            }
        });

        // Bind to injector
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                EntityManagerFactory entityManagerFactory = Util.getEntityManagerFactory(settings.getModelPackageName(),
                        settings.enableAsync(), optionalCompiler, settings.getDatabaseProperties());

                EntityDictionary dictionary = settings.getEntityDictionary(injector, optionalCompiler);

                MetaDataStore metaDataStore = settings.getMetaDataStore(optionalCompiler);

                QueryEngine queryEngine = settings.getQueryEngine(metaDataStore, entityManagerFactory);

                DataStore dataStore = settings.getDataStore(
                        metaDataStore,
                        settings.getAggregationDataStore(queryEngine, optionalCompiler),
                        entityManagerFactory);

                ElideSettings elideSettings = settings.getElideSettings(dictionary, dataStore);

                Elide elide = new Elide(elideSettings);

                // Bind elide instance for injection into endpoint
                bind(elide).to(Elide.class).named("elide");

                // Bind additional elements
                bind(elideSettings).to(ElideSettings.class);
                bind(elideSettings.getDictionary()).to(EntityDictionary.class);
                bind(elideSettings.getDataStore()).to(DataStore.class).named("elideDataStore");

                // Binding async service
                if (settings.enableAsync()) {
                    AsyncQueryDAO asyncQueryDao = settings.getAsyncQueryDAO();
                    if (asyncQueryDao == null) {
                        asyncQueryDao = new DefaultAsyncQueryDAO(elide, elide.getDataStore());
                    }
                    bind(asyncQueryDao).to(AsyncQueryDAO.class);

                    AsyncExecutorService.init(elide, settings.getAsyncThreadSize(),
                            settings.getAsyncMaxRunTimeMinutes(), asyncQueryDao);
                    bind(AsyncExecutorService.getInstance()).to(AsyncExecutorService.class);

                    // Binding AsyncQuery LifeCycleHook
                    ExecuteQueryHook executeQueryHook = new ExecuteQueryHook(AsyncExecutorService.getInstance());
                    UpdatePrincipalNameHook updatePrincipalNameHook = new UpdatePrincipalNameHook();

                    dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, executeQueryHook, false);
                    dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, updatePrincipalNameHook, false);

                    // Binding async cleanup service
                    if (settings.enableAsyncCleanup()) {
                        AsyncCleanerService.init(elide, settings.getAsyncMaxRunTimeMinutes(),
                                settings.getAsyncQueryCleanupDays(), asyncQueryDao);
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
