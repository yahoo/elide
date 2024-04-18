/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.standalone;

import static com.paiondata.elide.standalone.config.ElideResourceConfig.ASYNC_EXECUTOR_ATTR;
import static com.paiondata.elide.standalone.config.ElideResourceConfig.ASYNC_UPDATER_ATTR;
import static com.paiondata.elide.standalone.config.ElideResourceConfig.ELIDE_STANDALONE_SETTINGS_ATTR;

import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.standalone.config.ElideResourceConfig;
import com.paiondata.elide.standalone.config.ElideStandaloneSettings;
import com.paiondata.elide.standalone.config.ElideStandaloneSubscriptionSettings;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.servlet.ServletContainer;

import io.dropwizard.metrics.servlet.InstrumentedFilter;
import io.dropwizard.metrics.servlets.AdminServlet;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Elide Standalone.
 */
@Slf4j
public class ElideStandalone {
    private final ElideStandaloneSettings elideStandaloneSettings;
    private Server jettyServer;

    /**
     * Constructor.
     *
     * @param elideStandaloneSettings Elide standalone configuration settings.
     */
    public ElideStandalone(ElideStandaloneSettings elideStandaloneSettings) {
        this.elideStandaloneSettings = elideStandaloneSettings;
    }

    /**
     * Constructor.
     *
     * @param checkMappings Check mappings to use for service.
     */
    public ElideStandalone(Map<String, Class<? extends Check>> checkMappings) {
        this(new ElideStandaloneSettings() {
            @Override
            public Map<String, Class<? extends Check>> getCheckMappings() {
                return checkMappings;
            }
        });
    }
    /**
     * Start the Elide service.
     * This method blocks until the server exits.
     *
     * @throws Exception Exception thrown
     */
    public void start() throws Exception {
        start(true);

    }
    /**
     * Start the Elide service.
     *
     * @param block - Whether or not to wait for the server to shutdown.
     * @throws Exception Exception thrown
     */
    public void start(boolean block) throws Exception {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        log.info("Starting jetty server on port: {}", elideStandaloneSettings.getPort());
        jettyServer = new Server(elideStandaloneSettings.getPort());
        jettyServer.setHandler(context);

        context.setAttribute(ELIDE_STANDALONE_SETTINGS_ATTR, elideStandaloneSettings);

        if (elideStandaloneSettings.getAsyncProperties().enabled()) {
            Integer threadPoolSize = elideStandaloneSettings.getAsyncProperties().getThreadSize() == null
                            ? AsyncExecutorService.DEFAULT_THREAD_POOL_SIZE
                            : elideStandaloneSettings.getAsyncProperties().getThreadSize();
            context.setAttribute(ASYNC_EXECUTOR_ATTR, Executors.newFixedThreadPool(threadPoolSize));
            context.setAttribute(ASYNC_UPDATER_ATTR, Executors.newFixedThreadPool(threadPoolSize));
        }

        if (elideStandaloneSettings.enableJsonApi()) {
            ServletHolder jerseyServlet = context.addServlet(ServletContainer.class,
                    elideStandaloneSettings.getJsonApiPathSpec());
            jerseyServlet.setInitOrder(0);
            jerseyServlet.setInitParameter("jersey.config.server.provider.packages",
                    "com.paiondata.elide.jsonapi.resources");
            jerseyServlet.setInitParameter("jakarta.ws.rs.Application", ElideResourceConfig.class.getCanonicalName());
        }

        if (elideStandaloneSettings.enableGraphQL()) {
            ServletHolder jerseyServlet = context.addServlet(ServletContainer.class,
                    elideStandaloneSettings.getGraphQLApiPathSpec());
            jerseyServlet.setInitOrder(0);
            jerseyServlet.setInitParameter("jersey.config.server.provider.packages", "com.paiondata.elide.graphql");
            jerseyServlet.setInitParameter("jakarta.ws.rs.Application", ElideResourceConfig.class.getCanonicalName());
        }
        ElideStandaloneSubscriptionSettings subscriptionSettings = elideStandaloneSettings.getSubscriptionProperties();
        if (elideStandaloneSettings.enableGraphQL() && subscriptionSettings.enabled()) {
            // GraphQL subscription endpoint
            JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, serverContainer) -> {
                serverContainer.addEndpoint(subscriptionSettings.serverEndpointConfig(elideStandaloneSettings, false));
            });
            JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, serverContainer) -> {
                serverContainer.addEndpoint(subscriptionSettings.serverEndpointConfig(elideStandaloneSettings, true));
            });
        }

        if (elideStandaloneSettings.getAsyncProperties().enableExport()) {
            ServletHolder jerseyServlet = context.addServlet(ServletContainer.class,
                    elideStandaloneSettings.getAsyncProperties().getExportApiPathSpec());
            jerseyServlet.setInitOrder(0);
            jerseyServlet.setInitParameter("jersey.config.server.provider.packages",
                    "com.paiondata.elide.async.resources");
            jerseyServlet.setInitParameter("jakarta.ws.rs.Application", ElideResourceConfig.class.getCanonicalName());
        }

        if (elideStandaloneSettings.enableServiceMonitoring()) {
            FilterHolder instrumentedFilterHolder = new FilterHolder(InstrumentedFilter.class);
            instrumentedFilterHolder.setName("instrumentedFilter");
            instrumentedFilterHolder.setAsyncSupported(true);
            context.addFilter(instrumentedFilterHolder,
                    "/*",
                    EnumSet.of(DispatcherType.REQUEST));

            context.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY,
                    ElideResourceConfig.getHealthCheckRegistry());
            context.setAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE, ElideResourceConfig.getMetricRegistry());

            context.setAttribute(MetricsServlet.METRICS_REGISTRY, ElideResourceConfig.getMetricRegistry());
            context.addServlet(AdminServlet.class, "/stats/*");
        }

        if (elideStandaloneSettings.enableApiDocs()) {
            ServletHolder jerseyServlet = context.addServlet(ServletContainer.class,
                    elideStandaloneSettings.getApiDocsPathSpec());
            jerseyServlet.setInitOrder(0);
            jerseyServlet.setInitParameter("jersey.config.server.provider.packages",
                    "com.paiondata.elide.swagger.resources");
            jerseyServlet.setInitParameter("jakarta.ws.rs.Application", ElideResourceConfig.class.getCanonicalName());
        }

        elideStandaloneSettings.updateServletContextHandler(context);

        try {
            jettyServer.start();
            log.info("Jetty started!");
            if (block) {
                jettyServer.join();
            }
        } catch (Exception e) {
            log.error("Unexpected exception caught: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (block) {
                jettyServer.destroy();
            }
        }
    }

    /**
     * Stop the Elide service.
     *
     * @throws Exception Exception thrown
     */
    public void stop() throws Exception {
        jettyServer.stop();
        jettyServer.destroy();
    }
}
