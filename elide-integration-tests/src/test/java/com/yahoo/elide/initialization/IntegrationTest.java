/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.jayway.restassured.RestAssured;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.test.DataStoreHarness;
import com.yahoo.elide.jsonapi.JsonApiMapper;

import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test initializer.
 *
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest {
    /**
     * The constant dataStore.
     */
    public DataStore dataStore = null;
    private static volatile Server server = null;
    private final String resourceConfig;
    private String packageName;
    private static DataStoreHarness dataStoreHarness;


    /**
     * The Json api mapper.
     * Empty dictionary is OK provided the OBJECT_MAPPER is used for reading only
     */
    protected final JsonApiMapper jsonApiMapper = new JsonApiMapper(new EntityDictionary(new HashMap<>()));

    protected IntegrationTest(final Class<? extends ResourceConfig> resourceConfig, String packageName) {
        this.resourceConfig = resourceConfig.getCanonicalName();
        this.packageName = packageName;
        this.dataStore = getNewDatabaseManager();
    }

    public DataStore getNewDatabaseManager() {
        try {
            final String dataStoreSupplierName = System.getProperty("dataStoreHarness");
            dataStoreHarness = Class.forName(dataStoreSupplierName).asSubclass(DataStoreHarness.class).newInstance();
            return dataStoreHarness.getDataStore();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalStateException(e);
        }
    }

    public static DataStore getDataBaseManager() {
        return dataStoreHarness.getDataStore();
    }

    @BeforeAll
    public void hibernateInit() throws Exception {
        setUpServer();
    }

    @AfterEach
    public void cleanseTestData() {
        dataStoreHarness.cleanseTestData();
    }

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

    @AfterAll
    public final void tearDownServer() {
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
