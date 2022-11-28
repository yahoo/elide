/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.async.integration.tests.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.test.jsonapi.elements.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;

import io.restassured.response.Response;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.Executors;

import javax.ws.rs.core.MediaType;

/**
 * Parent class with common code for AsyncIT and TableExportIT.
 */
public abstract class AsyncApiIT extends IntegrationTest {
    @Getter private Integer port;
    private String apiPath;

    private static final Resource ENDERS_GAME = resource(
            type("book"),
            attributes(
                    attr("title", "Ender's Game"),
                    attr("genre", "Science Fiction"),
                    attr("language", "English")
            )
    );

    private static final Resource GAME_OF_THRONES = resource(
            type("book"),
            attributes(
                    attr("title", "Song of Ice and Fire"),
                    attr("genre", "Mythology Fiction"),
                    attr("language", "English")
            )
    );

    private static final Resource FOR_WHOM_THE_BELL_TOLLS = resource(
            type("book"),
            attributes(
                    attr("title", "For Whom the Bell Tolls"),
                    attr("genre", "Literary Fiction"),
                    attr("language", "English")
            )
    );

    public AsyncApiIT(String apiPath) {
        super(AsyncIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());

        this.port = super.getRestAssuredPort();
        this.apiPath = apiPath;
    }

    @Override
    public void modifyServletContextHandler() {
        // Set Attributes to be fetched in AsyncIntegrationTestApplicationResourceConfig
        this.servletContextHandler.setAttribute(AsyncIntegrationTestApplicationResourceConfig.ASYNC_EXECUTOR_ATTR, Executors.newFixedThreadPool(5));
    }

    @Override
    protected DataStoreTestHarness createHarness() {
        DataStoreTestHarness dataStoreTestHarness = super.createHarness();
        return new DataStoreTestHarness() {
                @Override
                public DataStore getDataStore() {
                    return new AsyncDelayDataStore(dataStoreTestHarness.getDataStore());
                }
                @Override
                public void cleanseTestData() {
                    dataStoreTestHarness.cleanseTestData();
                }
        };
    }
    /**
     * Creates test data for all tests.
     */
    @BeforeEach
    public void init() {
        //Create Book: Ender's Game
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(ENDERS_GAME).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(GAME_OF_THRONES).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(FOR_WHOM_THE_BELL_TOLLS).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
    }

    public Response getJSONAPIResponse(String id) throws InterruptedException {
        Response response = null;
        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            response = given()
                    .accept("application/vnd.api+json")
                    .get("/" + apiPath + "/" + id);

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Async API is completed
            if (outputResponse.equals("COMPLETE")) {
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async API Request has failed.");
            i++;
            assertNotEquals(1000, i, "Async API Request not completed.");
        }

        return response;
    }

    public String getGraphQLResponse(String id, String additionalResultColumns) throws InterruptedException {
        String  responseGraphQL = null;
        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            responseGraphQL = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{\"query\":\"{ " + apiPath + "(ids: [\\\"" + id + "\\\"]) "
                            + "{ edges { node { id queryType status result "
                            + "{ " + additionalResultColumns + " httpStatus recordCount } } } } }\","
                            + "\"variables\":null}")
                    .post("/graphQL")
                    .asString();
            // If Async API Request is created and completed
            if (responseGraphQL.contains("\"status\":\"COMPLETE\"")) {
                break;
            }
            assertTrue(responseGraphQL.contains("\"status\":\"PROCESSING\""), "Async API Request has failed.");
            i++;
            assertNotEquals(1000, i, "Async API Request not completed.");
        }

        return responseGraphQL;
    }

    public JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return graphqlNode;
    }
}
