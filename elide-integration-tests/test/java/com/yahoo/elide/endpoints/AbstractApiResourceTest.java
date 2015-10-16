/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import com.yahoo.elide.resources.JsonApiEndpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.io.InputStream;

/**
 * AbstractApiResource Test
 */
@Slf4j
public class AbstractApiResourceTest {
    private Server server;
    private String resourceConfig;

    private final ObjectMapper mapper = new ObjectMapper();

    public AbstractApiResourceTest(Class<? extends ResourceConfig> resourceConfig) {
        this.resourceConfig = resourceConfig.getCanonicalName();
    }

    public AbstractApiResourceTest() {
        this.resourceConfig = TestApplicationResourceConfig.class.getCanonicalName();
    }

    @BeforeClass
    public final void setUpServer() throws Exception {
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
        servletHolder.setInitParameter("jersey.config.server.provider.packages",
                JsonApiEndpoint.class.getPackage().getName());
        servletHolder.setInitParameter("javax.ws.rs.Application",
                resourceConfig);

        log.debug("...Starting Server...");
        server.start();
    }

    @AfterClass
    public final void tearDownServer() {
        log.debug("...Stopping Server...");
        try {
            server.stop();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    /**
     * Parse provided string into JsonNode
     * @param jsonString provided JSON
     * @return JsonNode representation
     */
    public JsonNode toJsonNode(String jsonString) {
        try {
            return mapper.readTree(jsonString);
        } catch (IOException e) {
            Assert.fail("Unable to parse JSON\n" + jsonString, e);
            throw new IllegalStateException(); // should not reach here
        }
    }

    /**
     * Read resource as a JSON string
     * @param  resourceName name of the desired resource
     * @return JSON string
     */
    public String getJson(String resourceName) {
        try (InputStream is = AbstractApiResourceTest.class.getResourceAsStream(resourceName)) {
            return String.valueOf(mapper.readTree(is));
        } catch (IOException e) {
            Assert.fail("Unable to open test data " + resourceName, e);
            throw new IllegalStateException(); // should not reach here
        }
    }
}
