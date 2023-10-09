/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.tests;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient;
import com.yahoo.elide.jsonapi.JsonApi;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import graphql.ExecutionResult;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.net.URI;
import java.util.List;

public class SubscriptionTest extends IntegrationTest {

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
}
