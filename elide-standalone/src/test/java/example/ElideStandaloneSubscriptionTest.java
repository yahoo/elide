/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.paiondata.elide.Serdes.SerdesBuilder;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.paiondata.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient;
import com.paiondata.elide.graphql.GraphQLExceptionHandler;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.standalone.ElideStandalone;
import com.paiondata.elide.standalone.config.ElideStandaloneSubscriptionSettings;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import graphql.ExecutionResult;
import jakarta.jms.ConnectionFactory;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Tests ElideStandalone starts and works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneSubscriptionTest extends ElideStandaloneTest {
    protected EmbeddedActiveMQ embedded;
    protected GraphQLExceptionHandler graphqlExceptionHandler;
    protected ISO8601DateSerde serde = spy(new ISO8601DateSerde("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")));

    @BeforeAll
    public void init() throws Exception {
        settings = new ElideStandaloneTestSettings() {

            @Override
            public SerdesBuilder getSerdesBuilder() {
                return super.getSerdesBuilder().entry(Date.class, serde);
            }

            @Override
            public GraphQLExceptionHandler getGraphQLExceptionHandler() {
                graphqlExceptionHandler = spy(super.getGraphQLExceptionHandler());
                return graphqlExceptionHandler;
            }

            @Override
            public ElideStandaloneSubscriptionSettings getSubscriptionProperties() {
                return new ElideStandaloneSubscriptionSettings() {
                    @Override
                    public boolean enabled() {
                        return true;
                    }

                    @Override
                    public ConnectionFactory getConnectionFactory() {
                        return new ActiveMQConnectionFactory("vm://0");
                    }
                };
            }
        };

        elide = new ElideStandalone(settings);
        elide.start(false);
    }

    @BeforeAll
    public void initArtemis() throws Exception {
        //Startup up an embedded active MQ.
        embedded = new EmbeddedActiveMQ();
        Configuration configuration = new ConfigurationImpl();
        configuration.addAcceptorConfiguration("default", "vm://0");
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.setJournalType(JournalType.NIO);

        embedded.setConfiguration(configuration);
        embedded.start();
    }

    @BeforeEach
    public void resetMocks() {
        reset(graphqlExceptionHandler);
        reset(serde);
    }

    @AfterAll
    public void shutdown() throws Exception {
        embedded.stop();
        elide.stop();
    }

    @Test
    public void testSubscription() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {post(topic: ADDED) {id content}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:" + settings.getPort() + "/subscription"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            given()
                    .contentType(JsonApi.MEDIA_TYPE)
                    .accept(JsonApi.MEDIA_TYPE)
                    .body(
                            datum(
                                    resource(
                                            type("post"),
                                            id("3"),
                                            attributes(
                                                    attr("content", "This is my first post. woot."),
                                                    attr("date", "2019-01-01T00:00Z")
                                            )
                                    )
                            )
                    )
                    .post("/api/post")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);

            List<ExecutionResult> results = client.waitOnClose(10);

            client.sendClose();

            assertEquals(1, results.size());
            assertEquals(0, results.get(0).getErrors().size());
        }
    }

    @Test
    public void testSubscriptionApiVersion() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {post(topic: ADDED) {id text}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:" + settings.getPort() + "/subscription/v1.0"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            given()
                    .contentType(JsonApi.MEDIA_TYPE)
                    .accept(JsonApi.MEDIA_TYPE)
                    .body(
                            datum(
                                    resource(
                                            type("post"),
                                            id("99"),
                                            attributes(
                                                    attr("text", "This is my first post. woot."),
                                                    attr("date", "2019-01-01T00:00Z")
                                            )
                                    )
                            )
                    )
                    .post("/api/v1.0/post")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);

            List<ExecutionResult> results = client.waitOnClose(10);

            client.sendClose();

            assertEquals(1, results.size());
            assertEquals(0, results.get(0).getErrors().size());
        }
    }

    @Test
    public void graphqlExceptionHandlerShouldBeCalled() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {post(topic: ADDED) {id texta}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:" + settings.getPort() + "/subscription/v1.0"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);
            client.sendClose();
        }

        verify(graphqlExceptionHandler).handleException(assertArg(arg -> {
            assertInstanceOf(InvalidEntityBodyException.class, arg);
            assertEquals("Bad Request Body'Unknown attribute field {post.texta}.'", arg.getMessage());
        }), any());
    }

    @Test
    public void serdeShouldBeCalled() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {post(topic: ADDED) {id text date}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:" + settings.getPort() + "/subscription/v1.0"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            given()
                    .contentType(JsonApi.MEDIA_TYPE)
                    .accept(JsonApi.MEDIA_TYPE)
                    .body(
                            datum(
                                    resource(
                                            type("post"),
                                            id("95"),
                                            attributes(
                                                    attr("text", "This is my second post. woot."),
                                                    attr("date", "2019-01-01T00:00Z")
                                            )
                                    )
                            )
                    )
                    .post("/api/v1.0/post")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);

            List<ExecutionResult> results = client.waitOnClose(10);

            client.sendClose();

            assertEquals(1, results.size());
            assertEquals(0, results.get(0).getErrors().size());
            verify(serde, atLeast(1)).deserialize(assertArg(arg -> {
                assertEquals("2019-01-01T00:00Z", arg);
            }));
            Date expected = Date.from(Instant.parse("2019-01-01T00:00:00Z"));
            verify(serde, atLeast(1)).serialize(assertArg(arg -> {
                assertEquals(expected, arg);
            }));
        }
    }
}
