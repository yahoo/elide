/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.test.graphql.GraphQLDSL.UNQUOTED_VALUE;
import static com.yahoo.elide.test.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.test.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.test.graphql.GraphQLDSL.document;
import static com.yahoo.elide.test.graphql.GraphQLDSL.field;
import static com.yahoo.elide.test.graphql.GraphQLDSL.mutation;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.integration.tests.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.jsonapi.resources.SecurityContextUser;
import com.yahoo.elide.test.graphql.EnumFieldSerializer;
import com.yahoo.elide.test.jsonapi.elements.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TableExportIT extends IntegrationTest {
    private Integer port;

    @Data
    private class TableExport {
        private String id;
        private String query;

        @JsonSerialize(using = EnumFieldSerializer.class, as = String.class)
        private String queryType;
        private Integer asyncAfterSeconds;
        @JsonSerialize(using = EnumFieldSerializer.class, as = String.class)
        private String resultType;

        @JsonSerialize(using = EnumFieldSerializer.class, as = String.class)
        private String status;
    }

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

    public TableExportIT() {
        super(AsyncIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());

        this.port = super.getPort();
    }

    @Override
    protected DataStoreTestHarness createHarness() {
        DataStoreTestHarness dataStoreTestHarness = super.createHarness();
        return new DataStoreTestHarness() {
                public DataStore getDataStore() {
                    return new AsyncDelayDataStore(dataStoreTestHarness.getDataStore(), 5000);
                }
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

    /**
     * Various tests for a JSONAPI query as a TableExport Request with asyncAfterSeconds value set to 0.
     * Happy Path Test Scenario 1
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void jsonApiHappyPath1() throws InterruptedException, IOException {

        AsyncDelayStoreTransaction.sleep = true;

        //Create Table Export Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("edc4a871-dff2-4054-804e-d80075cf830a"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "0"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED)
                .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf830a"))
                .body("data.type", equalTo("tableExport"))
                .body("data.attributes.status", equalTo("PROCESSING"))
                .body("data.attributes.result.recordCount", nullValue())
                .body("data.attributes.result.responseBody", nullValue())
                .body("data.attributes.result.httpStatus", nullValue());


        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/tableExport/edc4a871-dff2-4054-804e-d80075cf830a");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Table Export is created and completed
            if (outputResponse.equals("COMPLETE")) {

                // Validate TableExport  Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf830a"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", nullValue())
                        .body("data.attributes.result.recordCount", equalTo(3))
                        .body("data.attributes.result.url",
                                equalTo("http://localhost:" + port + "/export/edc4a871-dff2-4054-804e-d80075cf830a"))
                        .body("data.attributes.result.httpStatus", equalTo(200));

                assertEquals("\"title\"\n"
                        + "\"For Whom the Bell Tolls\"\n"
                        + "\"Song of Ice and Fire\"\n"
                        + "\"Ender's Game\"\n", getStoredFileContents("edc4a871-dff2-4054-804e-d80075cf830a"));

                break;
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Table Export has failed.");
                break;
            }

            i++;

            if (i == 1000) {
                fail("Table Export not completed.");
            }
        }
    }

    /**
     * Various tests for a JSONAPI query as a TableExport Request with asyncAfterSeconds value set to 7.
     * Happy Path Test Scenario 2
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void jsonApiHappyPath2() throws InterruptedException, IOException {

        AsyncDelayStoreTransaction.sleep = true;

        //Create TableExport Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("edc4a871-dff2-4054-804e-d80075cf831a"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "7"),
                                                attr("resultType", "JSON")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED)
                .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf831a"))
                .body("data.type", equalTo("tableExport"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.attributes.result.message", nullValue())
                .body("data.attributes.result.recordCount", equalTo(3))
                .body("data.attributes.result.url",
                        equalTo("http://localhost:" + port + "/export/edc4a871-dff2-4054-804e-d80075cf831a"))
                .body("data.attributes.result.httpStatus", equalTo(200));

        assertEquals("[\n"
                + "{\"title\":\"For Whom the Bell Tolls\"}\n"
                + ",{\"title\":\"Song of Ice and Fire\"}\n"
                + ",{\"title\":\"Ender's Game\"}\n"
                + "]\n", getStoredFileContents("edc4a871-dff2-4054-804e-d80075cf831a"));

    }

    /**
     * Test for a GraphQL query as a Table Export Request with asyncAfterSeconds value set to 0.
     * Happy Path Test Scenario 1
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void graphQLHappyPath1() throws InterruptedException, IOException {

        AsyncDelayStoreTransaction.sleep = true;
        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf828e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("CSV");
        queryObj.setQuery("{\"query\":\"{ book { edges { node { title } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "tableExport",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType"),
                                                 field("status"),
                                                 field("resultType")
                                         )
                                 )
                         )
                 )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            String responseGraphQL = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{\"query\":\"{ tableExport(ids: [\\\"edc4a871-dff2-4054-804e-d80075cf828e\\\"]) "
                            + "{ edges { node { id queryType status result "
                            + "{ url httpStatus recordCount } } } } }\","
                            + "\"variables\":null}")
                    .post("/graphQL")
                    .asString();
            // If Table Export is created and completed
            if (responseGraphQL.contains("\"status\":\"COMPLETE\"")) {

                expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                        + "\"result\":{\"url\":\"http://localhost:" + port + "/export/edc4a871-dff2-4054-804e-d80075cf828e\","
                        + "\"httpStatus\":200,\"recordCount\":3}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                assertEquals("\"title\"\n"
                        + "\"Ender's Game\"\n"
                        + "\"Song of Ice and Fire\"\n"
                        + "\"For Whom the Bell Tolls\"\n", getStoredFileContents("edc4a871-dff2-4054-804e-d80075cf828e"));
                break;
            } else if (!(responseGraphQL.contains("\"status\":\"PROCESSING\""))) {
                fail("TableExport has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("TableExport not completed.");
            }
        }
    }
    /**
     * Test for a GraphQL query as a TableExport Request with asyncAfterSeconds value set to 7.
     * Happy Path Test Scenario 2
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void graphQLHappyPath2() throws InterruptedException, IOException {

        AsyncDelayStoreTransaction.sleep = true;
        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf829e");
        queryObj.setAsyncAfterSeconds(7);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("JSON");
        queryObj.setQuery("{\"query\":\"{ book { edges { node { title } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "tableExport",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType"),
                                                 field("status"),
                                                 field("resultType")
                                         )
                                 )
                         )
                 )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf829e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"JSON\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        String responseGraphQL = given()
                 .contentType(MediaType.APPLICATION_JSON)
                 .accept(MediaType.APPLICATION_JSON)
                 .body("{\"query\":\"{ tableExport(ids: [\\\"edc4a871-dff2-4054-804e-d80075cf829e\\\"]) "
                         + "{ edges { node { id queryType status result "
                         + "{ url httpStatus recordCount } } } } }\","
                         + "\"variables\":null}")
                 .post("/graphQL")
                 .asString();

        expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf829e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                 + "\"result\":{\"url\":\"http://localhost:" + port + "/export/edc4a871-dff2-4054-804e-d80075cf829e\","
                 + "\"httpStatus\":200,\"recordCount\":3}}}]}}}";

        assertEquals(expectedResponse, responseGraphQL);
        assertEquals("[\n"
                + "{\"title\":\"Ender's Game\"}\n"
                + ",{\"title\":\"Song of Ice and Fire\"}\n"
                + ",{\"title\":\"For Whom the Bell Tolls\"}\n"
                + "]\n", getStoredFileContents("edc4a871-dff2-4054-804e-d80075cf829e"));
    }

    /**
     * Test for QueryStatus Set to PROCESSING instead of Queued.
     */
    @Test
    public void graphQLTestCreateFailOnQueryStatus() {

        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf839e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("PROCESSING");
        queryObj.setResultType("CSV");
        queryObj.setQuery("{\"query\":\"{ book { edges { node { id title } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "tableExport",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", queryObj, UNQUOTED_VALUE)
                                        ),
                                        selections(
                                                field("id"),
                                                field("query"),
                                                field("queryType"),
                                                field("status")
                                        )
                                )
                        )
                )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String output = response.extract().body().asString();
        assertEquals(true, output.contains("errors"));
        assertEquals(true, output.contains("CreatePermission Denied"));
    }

    /**
     * Test for ResultType Set to Invalid.
     */
    @Test
    public void graphQLTestCreateFailOnResultType() {

        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf939e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("XLS");
        queryObj.setQuery("{\"query\":\"{ book { edges { node { id title } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "tableExport",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", queryObj, UNQUOTED_VALUE)
                                        ),
                                        selections(
                                                field("id"),
                                                field("query"),
                                                field("queryType"),
                                                field("status")
                                        )
                                )
                        )
                )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String output = response.extract().body().asString();
        assertEquals(true, output.contains("errors"));
        assertEquals(true, output.contains("data.resultType"));
    }

    /**
     * Various tests for an unknown collection (group) that does not exist JSONAPI query as a TableExport Request.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiUnknownRequestTests() throws InterruptedException {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"),
                                        attributes(
                                                attr("query", "/group?sort=genre&fields%5Bgroup%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/tableExport/ba31ca4e-ed8f-4be0-a0f3-12088fa9263b");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If TableExport is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {
                // Validate TableExport Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.recordCount", nullValue())
                        .body("data.attributes.result.url", nullValue())
                        .body("data.attributes.result.message", equalTo("Unknown collection group"))
                        .body("data.attributes.result.httpStatus", equalTo(200));

                break;
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Table Export has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("Table Export not completed.");
            }
        }
    }

    /**
     * Various tests for bad export JSONAPI query as a TableExport Request.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiBadExportQueryTests() throws InterruptedException {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("ba31ca5e-ed8f-4be0-a0f3-12088fa9263b"),
                                        attributes(
                                                // %5Bgroup%5B instead of 5Bgroup%5D
                                                attr("query", "/group?sort=genre&fields%5Bgroup%5B=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/tableExport/ba31ca5e-ed8f-4be0-a0f3-12088fa9263b");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If TableExport is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {
                // Validate TableExport Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca5e-ed8f-4be0-a0f3-12088fa9263b"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.recordCount", nullValue())
                        .body("data.attributes.result.url", nullValue())
                        .body("data.attributes.result.message", equalTo("Unknown collection group"))
                        .body("data.attributes.result.httpStatus", equalTo(200));

                break;
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Table Export has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("Table Export not completed.");
            }
        }
    }

    /**
     * Various tests for an unknown collection (group) that does not exist GrapqhQL query as a TableExport Request.
     * @throws InterruptedException
     */
    @Test
    public void graphqlUnknownRequestTests() throws InterruptedException {
        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf939e");
        queryObj.setAsyncAfterSeconds(7);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("CSV");
        queryObj.setQuery("{\"query\":\"{ group { edges { node { title } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "tableExport",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", queryObj, UNQUOTED_VALUE)
                                        ),
                                        selections(
                                                field("id"),
                                                field("query"),
                                                field("queryType"),
                                                field("status"),
                                                field("resultType")
                                        )
                                )
                        )
                )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf939e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ group { edges { node { title } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        String responseGraphQL = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{\"query\":\"{ tableExport(ids: [\\\"edc4a871-dff2-4054-804e-d80075cf939e\\\"]) "
                        + "{ edges { node { id queryType status result "
                        + "{ message httpStatus recordCount } } } } }\","
                        + "\"variables\":null}")
                .post("/graphQL")
                .asString();

       expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf939e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                + "\"result\":{\"message\":\"Bad Request Body'Unknown entity {group}.'\","
                + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

       assertEquals(expectedResponse, responseGraphQL);
    }

    /**
     * Tests for making a Async request for TableExport request that does not exist.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiBadRequestTests() throws InterruptedException {

        //JSON API bad request
        given()
                .accept("application/vnd.api+json")
                .get("/tableExport/ba31ca4e-ed8f-4be0-a0f3-12088fa9263a")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("errors[0].detail", equalTo("Unknown identifier ba31ca4e-ed8f-4be0-a0f3-12088fa9263a for tableExport"));

    }

    /**
     * Test for making a Async request for TableExport request that does not exist.
     * @throws InterruptedException
     */
    @Test
    public void graphQLBadRequestTests() throws InterruptedException {

      //GRAPHQL bad request
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{\"query\":\"{ tableExport(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263a\\\"]) "
                        + "{ edges { node { id createdOn updatedOn queryType status result "
                        + "{ url httpStatus recordCount } } } } }\""
                        + ",\"variables\":null}")
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.tableExport", nullValue())
                .body("errors[0].message", equalTo("Exception while fetching data (/tableExport) : Unknown identifier "
                        + "[ba31ca4e-ed8f-4be0-a0f3-12088fa9263a] for tableExport"));

    }

    /**
     * Test for making a TableExport request to a model with relationship.
     * @throws InterruptedException
     */
    @Test
    public void jsonAPIRelationshipFetchTests() throws InterruptedException {
        //Create Table Export Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("0b0dd4e6-9cdc-4bbc-8db2-5c1491c5ee1e"),
                                        attributes(
                                                attr("query", "/book"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/tableExport/0b0dd4e6-9cdc-4bbc-8db2-5c1491c5ee1e");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If TableExport is created and completed
            if (outputResponse.equals("COMPLETE")) {
                // Validate TableExport Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("0b0dd4e6-9cdc-4bbc-8db2-5c1491c5ee1e"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.recordCount", nullValue())
                        .body("data.attributes.result.url", nullValue())
                        .body("data.attributes.result.message",
                                equalTo("Export is not supported for Query that requires traversing Relationships."))
                        .body("data.attributes.result.httpStatus", equalTo(200));

                break;
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Table Export has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("Table Export not completed.");
            }
        }
    }

    /**
     * Test for a GraphQL query as a Table Export Request with a Bad Export Query.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void graphQLBadExportQueryFail() throws InterruptedException {

        AsyncDelayStoreTransaction.sleep = true;
        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075df828e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("CSV");
        // Query missing }
        queryObj.setQuery("{\"query\":\"{ book { edges { node { title } } }\",\"variables\":null}");
        String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "tableExport",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType"),
                                                 field("status"),
                                                 field("resultType")
                                         )
                                 )
                         )
                 )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075df828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            String responseGraphQL = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{\"query\":\"{ tableExport(ids: [\\\"edc4a871-dff2-4054-804e-d80075df828e\\\"]) "
                            + "{ edges { node { id queryType status result "
                            + "{ message httpStatus recordCount } } } } }\","
                            + "\"variables\":null}")
                    .post("/graphQL")
                    .asString();
            // If TableExport is created and completed
            if (responseGraphQL.contains("\"status\":\"COMPLETE\"")) {

                expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075df828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                        + "\"result\":{\"message\":\"Bad Request Body'Can't parse query: { book { edges { node { title } } }'\","
                        + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            } else if (!(responseGraphQL.contains("\"status\":\"PROCESSING\""))) {
                fail("TableExport has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("TableExport not completed.");
            }
        }
    }

    /**
     * Test for a GraphQL query as a Table Export Request with Multiple Queries.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void graphQLMultipleQueryFail() throws InterruptedException {

        AsyncDelayStoreTransaction.sleep = true;
        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-def2-4054-804e-d80075cf828e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("CSV");
        // book and author queried from single query
        queryObj.setQuery("{\"query\":\"{ book { edges { node { title } } } author { edges { node { name } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "tableExport",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType"),
                                                 field("status"),
                                                 field("resultType")
                                         )
                                 )
                         )
                 )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-def2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } } author { edges { node { name } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            String responseGraphQL = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{\"query\":\"{ tableExport(ids: [\\\"edc4a871-def2-4054-804e-d80075cf828e\\\"]) "
                            + "{ edges { node { id queryType status result "
                            + "{ message httpStatus recordCount } } } } }\","
                            + "\"variables\":null}")
                    .post("/graphQL")
                    .asString();
            // If Table Export is created and completed
            if (responseGraphQL.contains("\"status\":\"COMPLETE\"")) {

                expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-def2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                        + "\"result\":{\"message\":\"Export is only supported for single Query with one root projection.\","
                        + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            } else if (!(responseGraphQL.contains("\"status\":\"PROCESSING\""))) {
                fail("TableExport has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("TableExport not completed.");
            }
        }
    }

    /**
     * Test for a GraphQL query as a Table Export Request with Multiple Projections.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void graphQLMultipleProjectionFail() throws InterruptedException {

        AsyncDelayStoreTransaction.sleep = true;
        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-daf2-4054-804e-d80075cf828e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("CSV");
        // 2 Separate Queries for book and author
        queryObj.setQuery("{\"query\":\"{ book { edges { node { title } } } } { author { edges { node { name } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "tableExport",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType"),
                                                 field("status"),
                                                 field("resultType")
                                         )
                                 )
                         )
                 )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-daf2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } } } { author { edges { node { name } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            String responseGraphQL = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{\"query\":\"{ tableExport(ids: [\\\"edc4a871-daf2-4054-804e-d80075cf828e\\\"]) "
                            + "{ edges { node { id queryType status result "
                            + "{ message httpStatus recordCount } } } } }\","
                            + "\"variables\":null}")
                    .post("/graphQL")
                    .asString();
            // If Table Export is created and completed
            if (responseGraphQL.contains("\"status\":\"COMPLETE\"")) {

                expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-daf2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                        + "\"result\":{\"message\":\"Export is only supported for single Query with one root projection.\","
                        + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            } else if (!(responseGraphQL.contains("\"status\":\"PROCESSING\""))) {
                fail("TableExport has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("TableExport not completed.");
            }
        }
    }

    /**
     * Test for a GraphQL query as a Table Export Request with Relationship fetch.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void graphQLRelationshipFetchFail() throws InterruptedException {

        AsyncDelayStoreTransaction.sleep = true;
        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dbf2-4054-804e-d80075cf828e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("CSV");
        queryObj.setQuery("{\"query\":\"{ book { edges { node { title authors {edges { node { name } } } } } } }\", \"variables\":null}");
        String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "tableExport",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType"),
                                                 field("status"),
                                                 field("resultType")
                                         )
                                 )
                         )
                 )
        ).toQuery();

        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dbf2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title authors {edges { node { name } } } } } } }\\\", \\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            String responseGraphQL = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{\"query\":\"{ tableExport(ids: [\\\"edc4a871-dbf2-4054-804e-d80075cf828e\\\"]) "
                            + "{ edges { node { id queryType status result "
                            + "{ message httpStatus recordCount } } } } }\","
                            + "\"variables\":null}")
                    .post("/graphQL")
                    .asString();
            // If Table Export is created and completed
            if (responseGraphQL.contains("\"status\":\"COMPLETE\"")) {

                expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dbf2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                        + "\"result\":{\"message\":\"Export is not supported for Query that requires traversing Relationships.\","
                        + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            } else if (!(responseGraphQL.contains("\"status\":\"PROCESSING\""))) {
                fail("TableExport has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("TableExport not completed.");
            }
        }
    }

    /**
     * Test for making a TableExport request to a model to which the user does not have permissions.
     * It will finish successfully but only with header.
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void noReadEntityTests() throws InterruptedException, IOException {
        // Load Forbidden Book details
        Resource bannedBook = resource(
                type("forbiddenBook"),
                attributes(
                        attr("title", "Banned Book"),
                        attr("genre", "Literary Fiction"),
                        attr("language", "English")
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(bannedBook).toJSON()
                )
                .post("/forbiddenBook")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        //Create Table Export Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e"),
                                        attributes(
                                                attr("query", "/forbiddenBook"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/tableExport/0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If TableExport is created and completed
            if (outputResponse.equals("COMPLETE")) {
                // Validate TableExport Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.recordCount", equalTo(0))
                        .body("data.attributes.result.url", notNullValue())
                        .body("data.attributes.result.message", nullValue())
                        .body("data.attributes.result.httpStatus", equalTo(200));

                // Only Header in the file, no records.
                String fileContents = getStoredFileContents("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e");

                assertEquals(1, fileContents.split("\n").length);
                assertTrue(fileContents.contains("genre"));
                break;
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Table Export has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("Table Export not completed.");
            }
        }
    }

    /**
     * Tests Read Permissions on TableExport Model for Admin Role.
     * @throws IOException IOException
     */
    @Test
    public void tableExportModelAdminReadPermissions() throws IOException {

        ElideResponse response = null;
        String id = "edc4a871-dff2-4054-804e-d80075c08959";
        String query = "test-query";

        com.yahoo.elide.async.models.TableExport queryObj = new com.yahoo.elide.async.models.TableExport();
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setResultType(ResultType.CSV);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setPrincipalName("owner-user");

        dataStore.populateEntityDictionary(
                        new EntityDictionary(AsyncIntegrationTestApplicationResourceConfig.MAPPINGS));
        DataStoreTransaction tx = dataStore.beginTransaction();
        tx.createObject(queryObj, null);
        tx.commit(null);
        tx.close();

        Elide elide = new Elide(new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(
                                        new EntityDictionary(AsyncIntegrationTestApplicationResourceConfig.MAPPINGS))
                        .withAuditLogger(new TestAuditLogger()).build());

        User ownerUser = new User(() -> "owner-user");
        SecurityContextUser securityContextAdminUser = new SecurityContextUser(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> "1";
            }
            @Override
            public boolean isUserInRole(String s) {
                return true;
            }
            @Override
            public boolean isSecure() {
                return false;
            }
            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        });
        SecurityContextUser securityContextNonAdminUser = new SecurityContextUser(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> "2";
            }
            @Override
            public boolean isUserInRole(String s) {
                return false;
            }
            @Override
            public boolean isSecure() {
                return false;
            }
            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        });

        String baseUrl = "/";
        // Principal is Owner
        response = elide.get(baseUrl, "/tableExport/" + id, new MultivaluedHashMap<>(), ownerUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        // Principal has Admin Role
        response = elide.get(baseUrl, "/tableExport/" + id, new MultivaluedHashMap<>(), securityContextAdminUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        // Principal without Admin Role
        response = elide.get(baseUrl, "/tableExport/" + id, new MultivaluedHashMap<>(), securityContextNonAdminUser, NO_VERSION);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getResponseCode());
    }

    /**
     * Various tests for a JSONAPI query as a TableExport Request with asyncAfterSeconds value set to 7.
     * asyncAfterSeconds is more than 10.
     * @throws InterruptedException
     */
    @Test
    public void asyncAfterBeyondMax() throws InterruptedException {
        String expected = "{\"errors\":[{\"detail\":\"Invalid value: Invalid Async After Seconds\"}]}";
        AsyncDelayStoreTransaction.sleep = true;

        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("edc4a871-dff2-4054-804e-e80075cf831f"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "70"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));

    }

    /**
     * Reset sleep delay flag after each test.
     */
    @AfterEach
    public void sleepDelayReset() {

        AsyncDelayStoreTransaction.sleep = false;
    }

    private JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return graphqlNode;
    }

    private String getStoredFileContents(String id) throws IOException {
        StringBuilder lines = new StringBuilder();

        try (Stream<String> stream = StreamSupport.stream(
                Files.readLines(new File("/tmp/" + id), StandardCharsets.UTF_8).spliterator(), false)) {
            stream.forEach(line -> lines.append(line).append(System.getProperty("line.separator")));
        }

        return lines.toString();
    }
}
