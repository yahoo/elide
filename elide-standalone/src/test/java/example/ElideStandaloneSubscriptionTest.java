/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneSubscriptionSettings;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import graphql.ExecutionResult;

import java.net.URI;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Tests ElideStandalone starts and works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneSubscriptionTest extends ElideStandaloneTest {
    protected EmbeddedActiveMQ embedded;

    @BeforeAll
    public void init() throws Exception {
        settings = new ElideStandaloneTestSettings() {

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
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
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
                    .post("/api/v1/post")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);

            List<ExecutionResult> results = client.waitOnClose(10);

            client.sendClose();

            assertEquals(1, results.size());
            assertEquals(0, results.get(0).getErrors().size());
        }
    }
}
