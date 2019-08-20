/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.resources.JsonApiEndpoint;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;

/**
 * Integration test initializer.  Tests are intended to run sequentially (so they don't stomp on each other's data).
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest {

    /* Shared between the test setup code (to insert test data) as well as the Jetty server (to serve test data) */
    private static DataStoreTestHarness dataStoreHarness;

    protected DataStore dataStore = null;
    private Server server = null;

    private final String resourceConfig;
    private String packageName;

    /**
     * The Json api mapper.
     * Empty dictionary is OK provided the OBJECT_MAPPER is used for reading only
     */
    protected final JsonApiMapper jsonApiMapper = new JsonApiMapper(new EntityDictionary(new HashMap<>()));

    protected IntegrationTest() {
        this(IntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    protected IntegrationTest(final Class<? extends ResourceConfig> resourceConfig, String packageName) {
        this.resourceConfig = resourceConfig.getCanonicalName();
        this.packageName = packageName;

        dataStoreHarness = createHarness();
        this.dataStore = dataStoreHarness.getDataStore();

        try {
            this.server = setUpServer();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private DataStoreTestHarness createHarness() {
        try {
            final String dataStoreSupplierName = System.getProperty("dataStoreHarness");

            if (dataStoreSupplierName != null && !dataStoreSupplierName.isEmpty()) {
                return Class.forName(dataStoreSupplierName).asSubclass(DataStoreTestHarness.class).newInstance();
            }
            return new InMemoryDataStoreHarness();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns an initialized data store.
     *
     * @return an initialized data store.
     */
    public static DataStore getDataStore() {
        if (dataStoreHarness == null) {
            //TODO - this is temporary code until AbstractIntegrationTestInitializer has been full replaced with
            //IntegrationTest.
            return AbstractIntegrationTestInitializer.getDatabaseManager();
        } else {
            return dataStoreHarness.getDataStore();
        }
    }

    @AfterEach
    public void afterEach() {

        //Delete all of the test data
        dataStoreHarness.cleanseTestData();
    }

    protected final Server setUpServer() throws Exception {
        // setup RestAssured
        RestAssured.baseURI = "http://localhost/";
        RestAssured.basePath = "/";

        // port randomly picked in pom.xml
        String restassuredPort = System.getProperty("restassured.port", System.getenv("restassured.port"));
        RestAssured.port =
                Integer.parseInt(restassuredPort != null && !restassuredPort.isEmpty() ? restassuredPort : "9999");

        // embedded jetty server
        Server server = new Server(RestAssured.port);
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

        return server;
    }

    @AfterAll
    public final void afterAll() {
        log.debug("...Stopping Server...");
        try {
            server.stop();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    protected void assertEqualDocuments(final String actual, final String expected) {
        try {
            JsonApiDocument expectedDoc = jsonApiMapper.readJsonApiDocument(expected);
            JsonApiDocument actualDoc = jsonApiMapper.readJsonApiDocument(actual);
            assertEquals(expectedDoc, actualDoc, "\n" + actual + "\n" + expected + "\n");
        } catch (IOException e) {
            fail("\n" + actual + "\n" + expected + "\n", e);
        }
    }
}
