/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator;
import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import example.Author;
import example.Book;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import graphql.ExecutionResult;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class JMSDataStoreIntegrationTest {
    private EmbeddedActiveMQ embedded;

    @BeforeAll
    public void init() throws Exception {

        //Startup up an embedded active MQ.
        embedded = new EmbeddedActiveMQ();
        Configuration configuration = new ConfigurationImpl();
        configuration.addAcceptorConfiguration("default", "vm://0");
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.setJournalType(JournalType.NIO);

        embedded.setConfiguration(configuration);
        embedded.start();

        //Start embedded Jetty
        setUpServer();
    }

    @AfterAll
    public void shutdown() throws Exception {
        embedded.stop();
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

        ServerEndpointConfig subscriptionEndpoint = ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, "/subscription")
                .configurator(SubscriptionWebSocketConfigurator.builder()
                        .baseUrl("/subscription")
                        .models(Set.of(ClassType.of(Book.class), ClassType.of(Author.class)))
                        .connectionFactory(new ActiveMQConnectionFactory("vm://0"))
                        .sendPingOnSubscribe(true)
                        .build())
                .build();
        container.addEndpoint(subscriptionEndpoint);

        log.debug("...Starting Server...");
        server.start();

        return server;
    }

    @Test
    public void testLifecycleEventBeforeSubscribe() throws Exception {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("book"),
                                        id("2"),
                                        attributes(attr("title", "foo"))
                                )
                        )
                )
                .post("/book")
                .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("2"));

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {book(topic: ADDED) {id title}}"));
        try (Session session = container.connectToServer(client, new URI("ws://localhost:9999/subscription"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            List<ExecutionResult> results = client.waitOnClose(1);

            assertEquals(0, results.size());
        }
    }

    @Test
    public void testLifecycleEventAfterSubscribe() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {book(topic: ADDED) {id title}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:9999/subscription"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

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


            List<ExecutionResult> results = client.waitOnClose(10);
            assertEquals(1, results.size());
        }
    }

    @Test
    public void testCreateUpdateAndDelete() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(3,
                List.of(
                        "subscription {book(topic: ADDED) {id title}}",
                        "subscription {book(topic: DELETED) {id title}}",
                        "subscription {book(topic: UPDATED) {id title}}"
                ));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:9999/subscription"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                            data(
                                    resource(
                                            type("book"),
                                            id("3"),
                                            attributes(attr("title", "foo"))
                                    )
                            )
                    )
                    .post("/book")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("3"));

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                            datum(
                                    resource(
                                            type("book"),
                                            id("3"),
                                            attributes(attr("title", "new title"))
                                    )
                            )
                    )
                    .patch("/book/3")
                    .then().statusCode(HttpStatus.SC_NO_CONTENT);

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .delete("/book/3")
                    .then().statusCode(HttpStatus.SC_NO_CONTENT);

            List<ExecutionResult> results = client.waitOnClose(300);

            assertEquals(3, results.size());
            System.out.println("Final Result: " + results);
        }
    }

    public static Integer getRestAssuredPort() {
        String restassuredPort = System.getProperty("restassured.port", System.getenv("restassured.port"));
        return Integer.parseInt(StringUtils.isNotEmpty(restassuredPort) ? restassuredPort : "9999");
    }
}
