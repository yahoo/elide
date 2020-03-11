/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.standalone.Util;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.contrib.swagger.SwaggerBuilder;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.models.Info;
import io.swagger.models.Swagger;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
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
     * Constructor
     *
     * @param injector Injection instance for application
     */
    @Inject
    public ElideResourceConfig(ServiceLocator injector, @Context ServletContext servletContext) {
        this.injector = injector;

        settings = (ElideStandaloneSettings) servletContext.getAttribute(ELIDE_STANDALONE_SETTINGS_ATTR);

        // Bind things that should be injectable to the Settings class
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(Util.combineModelEntities(settings.getModelPackageName(), settings.enableAsync())).to(Set.class)
                        .named("elideAllModels");
            }
        });

        // Bind to injector
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                ElideSettings elideSettings = settings.getElideSettings(injector);

                Elide elide = new Elide(elideSettings);

                // Bind elide instance for injection into endpoint
                bind(elide).to(Elide.class).named("elide");

                // Bind additional elements
                bind(elideSettings).to(ElideSettings.class);
                bind(elideSettings.getDictionary()).to(EntityDictionary.class);
                bind(elideSettings.getDataStore()).to(DataStore.class).named("elideDataStore");

                // Binding async service
                if(settings.enableAsync()) {
                    AsyncQueryDAO asyncQueryDao = settings.getAsyncQueryDAO();
                    asyncQueryDao.setElide(elide);
                    asyncQueryDao.setDataStore(elide.getDataStore());
                    bind(asyncQueryDao).to(AsyncQueryDAO.class);

                    AsyncExecutorService asyncExecService = new AsyncExecutorService(elide, settings.getAsyncThreadSize(),
                            settings.getMaxRunTimeMinutes(), asyncQueryDao);
                    bind(asyncExecService).to(AsyncExecutorService.class);

                    // Binding async cleanup service
                    if(settings.enableAsyncCleanup()) {
                        AsyncCleanerService asyncCleanerService = new AsyncCleanerService(elide, settings.getMaxRunTimeMinutes(),
                                 settings.getQueryCleanupDays(), asyncQueryDao);
                        bind(asyncCleanerService).to(AsyncCleanerService.class);
                    }
                }
            }
        });

        // Bind swaggers to given endpoint
        register(new org.glassfish.hk2.utilities.binding.AbstractBinder() {
            @Override
            protected void configure() {
                Map<String, Swagger> swaggerDocs = settings.enableSwagger();
                if (!swaggerDocs.isEmpty()) {
                    // Include the async models in swagger docs
                    if(settings.enableAsync()) {
                        EntityDictionary dictionary = new EntityDictionary(new HashMap());
                        dictionary.bindEntity(AsyncQuery.class);
                        dictionary.bindEntity(AsyncQueryResult.class);
                         
                        Info info = new Info().title("Async Service").version("1.0");

                        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);

                        Swagger swagger = builder.build().basePath("/api/v1");

                        swaggerDocs.put("async", swagger);
                    }

                    bind(swaggerDocs).named("swagger").to(new TypeLiteral<Map<String, Swagger>>() { });
                }
            }
        });

        registerFilters(settings.getFilters());

        additionalConfiguration(settings.getApplicationConfigurator());
    }

    /**
     * Init the supplemental resource config
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
