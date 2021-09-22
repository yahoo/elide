/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.graphql.subscriptions.hooks.TopicType;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import example.Author;
import example.Book;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class JMSDataStoreIntegrationTest {

    protected ConnectionFactory connectionFactory;
    protected EntityDictionary dictionary;
    protected JMSDataStore store;

    @BeforeAll
    public void init() throws Exception {
        EmbeddedActiveMQ embedded = new EmbeddedActiveMQ();
        Configuration configuration = new ConfigurationImpl();
        configuration.addAcceptorConfiguration("default", "vm://0");
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.setJournalType(JournalType.NIO);

        embedded.setConfiguration(configuration);
        embedded.start();

        setUpServer();
    }

    protected final Server setUpServer() throws Exception {
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

        // setup RestAssured
        RestAssured.baseURI = "http://localhost/";
        RestAssured.basePath = "/";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // port randomly picked in pom.xml
        RestAssured.port = getRestAssuredPort();

        // embedded jetty server
        Server server = new Server(RestAssured.port);
        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);

        //JSON API
        final ServletHolder servletHolder = servletContextHandler.addServlet(ServletContainer.class, "/*");
        servletHolder.setInitOrder(1);
        servletHolder.setInitParameter("jersey.config.server.provider.packages",
                JsonApiEndpoint.class.getPackage().getName());
        servletHolder.setInitParameter("javax.ws.rs.Application", TestResourceConfig.class.getName());

        //GraphQL API
        ServletHolder graphqlServlet = servletContextHandler.addServlet(ServletContainer.class, "/graphQL/*");
        graphqlServlet.setInitOrder(2);
        graphqlServlet.setInitParameter("jersey.config.server.provider.packages",
                com.yahoo.elide.graphql.GraphQLEndpoint.class.getPackage().getName());
        graphqlServlet.setInitParameter("javax.ws.rs.Application", TestResourceConfig.class.getName());

        // GraphQL subscription endpoint
        ServerContainer container  = WebSocketServerContainerInitializer.configureContext(servletContextHandler);

        ServerEndpointConfig echoConfig = ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class,"/subscription")
                .configurator(new SubscriptionWebSocketConfigurator())
                .build();
        container.addEndpoint(echoConfig);

        log.debug("...Starting Server...");
        server.start();

        return server;
    }


    @Test
    public void testJsonApiEndpoint() throws Exception {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("book"),
                                        id("1"),
                                        attributes(attr("title", "foo"))
                                )
                        )
                )
                .post("/book")
                .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("1"));
    }

    public static Integer getRestAssuredPort() {
        String restassuredPort = System.getProperty("restassured.port", System.getenv("restassured.port"));
        return Integer.parseInt(StringUtils.isNotEmpty(restassuredPort) ? restassuredPort : "9999");
    }
}
