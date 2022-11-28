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
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient;
import com.yahoo.elide.spring.controllers.JsonApiController;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import graphql.ExecutionResult;

import java.net.URI;
import java.util.List;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

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
                    .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
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
        }
    }
}
