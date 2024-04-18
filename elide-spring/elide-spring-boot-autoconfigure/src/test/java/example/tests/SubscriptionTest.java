/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.paiondata.elide.SerdesBuilderCustomizer;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.utils.coerce.converters.OffsetDateTimeSerde;
import com.paiondata.elide.core.utils.coerce.converters.Serde;
import com.paiondata.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient;
import com.paiondata.elide.graphql.GraphQLExceptionHandler;
import com.paiondata.elide.jsonapi.JsonApi;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;

import graphql.ExecutionResult;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

public class SubscriptionTest extends IntegrationTest {
    private static Serde<String, OffsetDateTime> serde = spy(new OffsetDateTimeSerde());

    @TestConfiguration
    public static class SerdeConfiguration {
        @Bean
        SerdesBuilderCustomizer serdesBuilderCustomzer() {
            return serdesBuilder -> {
                serdesBuilder.entry(OffsetDateTime.class, serde);
            };
        }
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void resetMocks() {
        reset(serde);
    }

    @SpyBean
    GraphQLExceptionHandler graphqlExceptionHandler;

    @Test
    public void testSubscription() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {group(topic: ADDED) {name commonName}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:" + port + "/subscription"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            given()
                    .contentType(JsonApi.MEDIA_TYPE)
                    .body(
                            datum(
                                    resource(
                                            type("group"),
                                            id("com.example.repository2"),
                                            attributes(
                                                    attr("commonName", "New group.")
                                            )
                                    )
                            )
                    )
                    .when()
                    .post("/json/group")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);


            List<ExecutionResult> results = client.waitOnClose(10);

            assertEquals(1, results.size());
            assertEquals(0, results.get(0).getErrors().size());

            when()
                    .delete("/json/group/com.example.repository2")
                    .then()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void testSubscriptionApiVersion() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {group(topic: ADDED) {name title}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:" + port + "/subscription/v1.0"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            given()
                    .contentType(JsonApi.MEDIA_TYPE)
                    .body(
                            datum(
                                    resource(
                                            type("group"),
                                            id("com.example.repository2.v1.0"),
                                            attributes(
                                                    attr("title", "New group.")
                                            )
                                    )
                            )
                    )
                    .when()
                    .post("/json/v1.0/group")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);


            List<ExecutionResult> results = client.waitOnClose(10);

            assertEquals(1, results.size());
            assertEquals(0, results.get(0).getErrors().size());

            when()
                    .delete("/json/v1.0/group/com.example.repository2.v1.0")
                    .then()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void graphqlExceptionHandlerShouldBeCalled() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {group(topic: ADDED) {name commonNamea}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:" + port + "/subscription"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);
        }

        verify(graphqlExceptionHandler).handleException(assertArg(arg -> {
            assertThat(arg).isInstanceOf(InvalidEntityBodyException.class);
            assertThat(arg).hasMessage("Bad Request Body'Unknown attribute field {group.commonNamea}.'");
        }), any());
    }

    @Test
    public void serdeShouldBeCalled() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        SubscriptionWebSocketTestClient client = new SubscriptionWebSocketTestClient(1,
                List.of("subscription {group(topic: ADDED) {name title createdOn}}"));

        try (Session session = container.connectToServer(client, new URI("ws://localhost:" + port + "/subscription/v3.0"))) {

            //Wait for the socket to be full established.
            client.waitOnSubscribe(10);

            given()
                    .contentType(JsonApi.MEDIA_TYPE)
                    .body(
                            datum(
                                    resource(
                                            type("group"),
                                            id("com.example.repository2.v3.0"),
                                            attributes(
                                                    attr("title", "New group."),
                                                    attr("createdOn", "2007-12-03T10:15:30+01:00")
                                            )
                                    )
                            )
                    )
                    .when()
                    .post("/json/v3.0/group")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);


            List<ExecutionResult> results = client.waitOnClose(10);

            assertEquals(1, results.size());
            assertEquals(0, results.get(0).getErrors().size());

            when()
                    .delete("/json/v3.0/group/com.example.repository2.v3.0")
                    .then()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            verify(serde, atLeast(1)).deserialize(assertArg(arg -> {
               assertThat(arg).isEqualTo("2007-12-03T10:15:30+01:00");
            }));

            OffsetDateTime expected = OffsetDateTime.parse("2007-12-03T10:15:30+01:00");
            verify(serde, atLeast(1)).serialize(assertArg(arg -> {
                assertEquals(expected.toInstant(), arg.toInstant());
            }));

        }
    }
}
