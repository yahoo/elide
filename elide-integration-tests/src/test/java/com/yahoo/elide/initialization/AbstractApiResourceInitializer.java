/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;

/**
 * Initialize API service.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractApiResourceInitializer {
    private static volatile Server server = null;
    private final String resourceConfig;
    private String packageName;

    public AbstractApiResourceInitializer() {
        this(IntegrationTestApplicationResourceConfig.class);
    }

    protected AbstractApiResourceInitializer(final Class<? extends ResourceConfig> resourceConfig, String packageName) {
        this.resourceConfig = resourceConfig.getCanonicalName();
        this.packageName = packageName;
    }

    protected AbstractApiResourceInitializer(final Class<? extends ResourceConfig> resourceConfig) {
        this(resourceConfig, JsonApiEndpoint.class.getPackage().getName());
    }

    @BeforeAll
    public final void setUpServer() throws Exception {
        if (server != null) {
            server.stop();
        }

        // setup RestAssured
        RestAssured.baseURI = "http://localhost/";
        RestAssured.basePath = "/";

        // port randomly picked in pom.xml
        String restassuredPort = System.getProperty("restassured.port", System.getenv("restassured.port"));
        RestAssured.port =
                Integer.parseInt(StringUtils.isNotEmpty(restassuredPort) ? restassuredPort : "9999");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // embedded jetty server
        server = new Server(RestAssured.port);
        final ServletContextHandler servletContextHandler =
                new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);

        final ServletHolder servletHolder = servletContextHandler.addServlet(ServletContainer.class, "/*");
        servletHolder.setInitOrder(1);
        servletHolder.setInitParameter("jersey.config.server.provider.packages", packageName);
        servletHolder.setInitParameter("javax.ws.rs.Application", resourceConfig);

        log.debug("...Starting Server...");
        server.start();
    }

    @AfterAll
    public final void tearDownServer() {
        log.debug("...Stopping Server...");
        try {
            server.stop();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
}
