/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone;

import static com.yahoo.elide.standalone.config.ElideResourceConfig.ELIDE_STANDALONE_SETTINGS_ATTR;

import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.standalone.config.ElideResourceConfig;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.SecurityContext;

@Slf4j
public class ElideStandalone {
    private final ElideStandaloneSettings elideStandaloneSettings;
    private Server jettyServer;

    /**
     * Constructor
     *
     * @param elideStandaloneSettings Elide standalone configuration settings.
     */
    public ElideStandalone(ElideStandaloneSettings elideStandaloneSettings) {
        this.elideStandaloneSettings = elideStandaloneSettings;
    }

    /**
     * Constructor
     *
     * @param checkMappings Check mappings to use for service.
     */
    public ElideStandalone(Map<String, Class<? extends Check>> checkMappings) {
        this(checkMappings, SecurityContext::getUserPrincipal);
    }

    /**
     * Constructor
     *
     * @param checkMappings Check mappings to use for service.
     * @param userExtractionFn User extraction function to use for service.
     */
    public ElideStandalone(Map<String, Class<? extends Check>> checkMappings,
                           DefaultOpaqueUserFunction userExtractionFn) {
        this(new ElideStandaloneSettings() {
            @Override
            public Map<String, Class<? extends Check>> getCheckMappings() {
                return checkMappings;
            }

            @Override
            public DefaultOpaqueUserFunction getUserExtractionFunction() {
                return userExtractionFn;
            }
        });
    }
    /**
     * Start the Elide service.
     *
     * This method blocks until the server exits.
     */
    public void start() throws Exception {
        start(true);

    }
    /**
     * Start the Elide service.
     *
     * @param block - Whether or not to wait for the server to shutdown.
     */
    public void start(boolean block) throws Exception {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        log.info("Starting jetty server on port: {}", elideStandaloneSettings.getPort());
        jettyServer = new Server(elideStandaloneSettings.getPort());
        jettyServer.setHandler(context);

        context.setAttribute(ELIDE_STANDALONE_SETTINGS_ATTR, elideStandaloneSettings);

        if (elideStandaloneSettings.enableJSONAPI()) {
            ServletHolder jerseyServlet = context.addServlet(ServletContainer.class,
                    elideStandaloneSettings.getJsonApiPathSpec());
            jerseyServlet.setInitOrder(0);
            jerseyServlet.setInitParameter("jersey.config.server.provider.packages", "com.yahoo.elide.resources");
            jerseyServlet.setInitParameter("javax.ws.rs.Application", ElideResourceConfig.class.getCanonicalName());
        }

        if (elideStandaloneSettings.enableGraphQL()) {
            ServletHolder jerseyServlet = context.addServlet(ServletContainer.class,
                    elideStandaloneSettings.getGraphQLApiPathSepc());
            jerseyServlet.setInitOrder(0);
            jerseyServlet.setInitParameter("jersey.config.server.provider.packages", "com.yahoo.elide.graphql");
            jerseyServlet.setInitParameter("javax.ws.rs.Application", ElideResourceConfig.class.getCanonicalName());
        }

        if (elideStandaloneSettings.enableServiceMonitoring()) {
            FilterHolder instrumentedFilterHolder = new FilterHolder(InstrumentedFilter.class);
            instrumentedFilterHolder.setName("instrumentedFilter");
            instrumentedFilterHolder.setAsyncSupported(true);
            context.addFilter(instrumentedFilterHolder,
                    "/*",
                    EnumSet.of(DispatcherType.REQUEST));

            context.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, ElideResourceConfig.getHealthCheckRegistry());
            context.setAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE, ElideResourceConfig.getMetricRegistry());
            context.setAttribute(MetricsServlet.METRICS_REGISTRY, ElideResourceConfig.getMetricRegistry());
            context.addServlet(AdminServlet.class, "/stats/*");
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
     */
    public void stop() throws Exception {
        jettyServer.stop();
        jettyServer.destroy();
    }
}
