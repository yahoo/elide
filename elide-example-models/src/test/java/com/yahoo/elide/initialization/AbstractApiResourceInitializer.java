/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.resources.JsonApiEndpoint;

import com.jayway.restassured.RestAssured;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import lombok.extern.slf4j.Slf4j;

/**
 * Initialize API service.
 */
@Slf4j
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

    @BeforeSuite
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
                Integer.parseInt(restassuredPort != null && !restassuredPort.isEmpty() ? restassuredPort : "9999");

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

        ServletHolder graphqlServlet = servletContextHandler.addServlet(ServletContainer.class, "/graphQL/*");
        graphqlServlet.setInitOrder(2);
        graphqlServlet.setInitParameter("jersey.config.server.provider.packages",
                com.yahoo.elide.graphql.GraphQLEndpoint.class.getPackage().getName());
        graphqlServlet.setInitParameter("javax.ws.rs.Application", resourceConfig);

        log.debug("...Starting Server...");
        server.start();
    }

    @AfterSuite
    public final void tearDownServer() {
        log.debug("...Stopping Server...");
        try {
            server.stop();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
}
