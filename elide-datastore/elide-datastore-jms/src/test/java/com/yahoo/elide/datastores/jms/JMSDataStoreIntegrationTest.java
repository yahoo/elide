/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.datastores.jms.TestBinder.EMBEDDED_JMS_URL;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator;
import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
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
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
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

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
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
        configuration.addAcceptorConfiguration("default", EMBEDDED_JMS_URL);
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
        JavaxWebSocketServletContainerInitializer.configure(servletContextHandler, (servletContext, serverContainer) ->
        {
            serverContainer.addEndpoint(ServerEndpointConfig.Builder
                    .create(SubscriptionWebSocket.class, "/subscription")
                    .configurator(SubscriptionWebSocketConfigurator.builder()
                            .baseUrl("/subscription")
                            .connectionFactory(new ActiveMQConnectionFactory(EMBEDDED_JMS_URL))
                            .sendPingOnSubscribe(true)
                            .build())
                    .build());
        });

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
    public void testLifecycleEventAfterSubscribeWithInaccessibleField() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {book(topic: ADDED) {id title nope}}"));

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
                                            id("14"),
                                            attributes(attr("title", "foo"))
                                    )
                            )
                    )
                    .post("/book")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("14"));


            List<ExecutionResult> results = client.waitOnClose(10);
            assertEquals(1, results.size());
            assertEquals("{book={id=14, title=foo, nope=null}}", results.get(0).getData().toString());
            assertEquals("[{ \"message\": \"Exception while fetching data (/book/nope) : ReadPermission Denied\", \"locations\": [SourceLocation{line=1, column=44}], \"path\": [book, nope]}]", results.get(0).getErrors().toString());
        }
    }

    @Test
    public void testLifecycleEventAfterSubscribeWithFilter() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {book(topic: ADDED, filter: \"title==foo\") {id title}}"));

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
                                            id("10"),
                                            attributes(attr("title", "bar"))
                                    )
                            )
                    )
                    .post("/book")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("10"));

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                            data(
                                    resource(
                                            type("book"),
                                            id("11"),
                                            attributes(attr("title", "foo"))
                                    )
                            )
                    )
                    .post("/book")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("11"));


            List<ExecutionResult> results = client.waitOnClose(10);
            assertEquals(1, results.size());
        }
    }

    @Test
    public void testLifecycleEventAfterSubscribeWithSecurityFilter() throws Exception {
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
                                            id("1000"),
                                            attributes(attr("title", "bar"))
                                    )
                            )
                    )
                    .post("/book")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("1000"));

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                            data(
                                    resource(
                                            type("book"),
                                            id("99"),
                                            attributes(attr("title", "foo"))
                                    )
                            )
                    )
                    .post("/book")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("99"));


            List<ExecutionResult> results = client.waitOnClose(10);
            assertEquals(1, results.size());
        }
    }

    @Test
    public void testCreateUpdateAndDelete() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(4,
                List.of(
                        "subscription {book(topic: ADDED) { id title authors { id name } publisher { id name }}}",
                        "subscription {book(topic: DELETED) { id title }}",
                        "subscription {book(topic: UPDATED) { id title publisher { id name }}}"
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
                                            type("publisher"),
                                            id("1"),
                                            attributes(attr("name", "Some Company"))
                                    )
                            )
                    )
                    .post("/publisher")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("1"));

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                            data(
                                    resource(
                                            type("author"),
                                            id("1"),
                                            attributes(attr("name", "Jane Doe"))
                                    )
                            )
                    )
                    .post("/author")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("1"));

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                            data(
                                    resource(
                                            type("book"),
                                            id("3"),
                                            attributes(attr("title", "foo")),
                                            relationships(
                                                    relation("authors", linkage(type("author"), id("1"))),
                                                    relation("publisher", linkage(type("publisher"), id("1")))
                                            )
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
                    .then().log().all().statusCode(HttpStatus.SC_NO_CONTENT);

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                            data(
                                    resource(
                                            type("publisher"),
                                            id("1")
                                    )
                            )
                    )
                    .delete("/book/3/relationships/publisher")
                    .then().log().all().statusCode(HttpStatus.SC_NO_CONTENT);

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .delete("/book/3")
                    .then().statusCode(HttpStatus.SC_NO_CONTENT);

            List<ExecutionResult> results = client.waitOnClose(300);

            assertEquals(4, results.size());
            assertEquals("{book={id=3, title=foo, authors=[{id=1, name=Jane Doe}], publisher={id=1, name=Some Company}}}", results.get(0).getData().toString());
            assertEquals("{book={id=3, title=new title, publisher={id=1, name=Some Company}}}", results.get(1).getData().toString());
            assertEquals("{book={id=3, title=new title, publisher=null}}", results.get(2).getData().toString());
            assertEquals("{book={id=3, title=new title}}", results.get(3).getData().toString());
        }
    }

    @Test
    public void testCustomSubscription() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(3,
                List.of("subscription {chat {id message}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:9999/subscription"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                            data(
                                    resource(
                                            type("chatBot"),
                                            id("1"),
                                            attributes(attr("name", "SocialBot"))
                                    )
                            )
                    )
                    .post("/chatBot")
                    .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("1"));

            List<ExecutionResult> results = client.waitOnClose(10);
            assertEquals(3, results.size());
            assertEquals("{chat={id=1, message=Hello!}}", results.get(0).getData().toString());
            assertEquals("{chat={id=2, message=How is your day?}}", results.get(1).getData().toString());
            assertEquals("{chat={id=3, message=My name is SocialBot}}", results.get(2).getData().toString());
        }
    }

    public static Integer getRestAssuredPort() {
        String restassuredPort = System.getProperty("restassured.port", System.getenv("restassured.port"));
        return Integer.parseInt(StringUtils.isNotEmpty(restassuredPort) ? restassuredPort : "9999");
    }
}
