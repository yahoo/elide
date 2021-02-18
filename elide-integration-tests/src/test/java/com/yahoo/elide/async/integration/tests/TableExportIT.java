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

import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.Executors;

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
    public void modifyServletContextHandler() {
        // Initialize Export End Point
        ServletHolder exportServlet = servletContextHandler.addServlet(ServletContainer.class, "/export/*");
        exportServlet.setInitOrder(3);
        exportServlet.setInitParameter("jersey.config.server.provider.packages",
                com.yahoo.elide.async.resources.ExportApiEndpoint.class.getPackage().getName());
        exportServlet.setInitParameter("javax.ws.rs.Application", AsyncIntegrationTestApplicationResourceConfig.class.getName());

        // Set Attributes to be fetched in AsyncIntegrationTestApplicationResourceConfig
        this.servletContextHandler.setAttribute(AsyncIntegrationTestApplicationResourceConfig.ASYNC_EXECUTOR_ATTR, Executors.newFixedThreadPool(5));
        try {
            this.servletContextHandler.setAttribute(AsyncIntegrationTestApplicationResourceConfig.STORAGE_DESTINATION_ATTR, Files.createTempDirectory("asyncIT"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected DataStoreTestHarness createHarness() {
        DataStoreTestHarness dataStoreTestHarness = super.createHarness();
        return new DataStoreTestHarness() {
                public DataStore getDataStore() {
                    return new AsyncDelayDataStore(dataStoreTestHarness.getDataStore());
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

    private Response getJSONAPIResponse(String id) throws InterruptedException {
        Response response = null;
        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            response = given()
                    .accept("application/vnd.api+json")
                    .get("/tableExport/" + id);

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Table Export is completed
            if (outputResponse.equals("COMPLETE")) {
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

        return response;
    }

    private String getGraphQLResponse(String id) throws InterruptedException {
        String  responseGraphQL = null;
        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            responseGraphQL = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{\"query\":\"{ tableExport(ids: [\\\"" + id + "\\\"]) "
                            + "{ edges { node { id queryType status result "
                            + "{ message url httpStatus recordCount } } } } }\","
                            + "\"variables\":null}")
                    .post("/graphQL")
                    .asString();
            // If Table Export is created and completed
            if (responseGraphQL.contains("\"status\":\"COMPLETE\"")) {
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

        return responseGraphQL;
    }

    /**
     * Various tests for a JSONAPI query as a TableExport Request with asyncAfterSeconds value set to 0.
     * Happy Path Test Scenario 1
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void jsonApiHappyPath1() throws InterruptedException, IOException {

        //Create Table Export Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .header("sleep", "1000")
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


        Response response = getJSONAPIResponse("edc4a871-dff2-4054-804e-d80075cf830a");

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
                + "\"Ender's Game\"\n", getStoredFileContents(port, "edc4a871-dff2-4054-804e-d80075cf830a"));
    }

    /**
     * Various tests for a JSONAPI query as a TableExport Request with asyncAfterSeconds value set to 7.
     * Happy Path Test Scenario 2
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void jsonApiHappyPath2() throws InterruptedException, IOException {

        //Create TableExport Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .header("sleep", "1000")
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
                + "]\n", getStoredFileContents(port, "edc4a871-dff2-4054-804e-d80075cf831a"));

    }

    /**
     * Test for a GraphQL query as a Table Export Request with asyncAfterSeconds value set to 0.
     * Happy Path Test Scenario 1
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void graphQLHappyPath1() throws InterruptedException, IOException {

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
                .header("sleep", "1000")
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        String responseGraphQL = getGraphQLResponse("edc4a871-dff2-4054-804e-d80075cf828e");
        expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                + "\"result\":{\"message\":null,\"url\":\"http://localhost:" + port + "/export/edc4a871-dff2-4054-804e-d80075cf828e\","
                + "\"httpStatus\":200,\"recordCount\":3}}}]}}}";

        assertEquals(expectedResponse, responseGraphQL);
        assertEquals("\"title\"\n"
                + "\"Ender's Game\"\n"
                + "\"Song of Ice and Fire\"\n"
                + "\"For Whom the Bell Tolls\"\n", getStoredFileContents(port, "edc4a871-dff2-4054-804e-d80075cf828e"));
    }

    /**
     * Test for a GraphQL query as a TableExport Request with asyncAfterSeconds value set to 7.
     * Happy Path Test Scenario 2
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void graphQLHappyPath2() throws InterruptedException, IOException {

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
                .header("sleep", "1000")
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
                + "]\n", getStoredFileContents(port, "edc4a871-dff2-4054-804e-d80075cf829e"));
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
        // Status should be QUEUED during submission.
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
     * Test for ResultType Set to an unsupported value.
     */
    @Test
    public void graphQLTestCreateFailOnUnSupportedResultType() {

        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf939e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        // XLS is not supported.
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
        System.out.println(output);
        assertEquals(true, output.contains("errors"));
        assertEquals(true, output.contains("Validation error of type WrongType: argument &#39;data.resultType&#39;"));
    }

    /**
     * Various tests for an unknown collection (group) that does not exist in TableExport Request JSONAPI query.
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
                                                // entity "group" does not exist.
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

        Response response = getJSONAPIResponse("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b");

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

        Response response = getJSONAPIResponse("ba31ca5e-ed8f-4be0-a0f3-12088fa9263b");
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

    }

    /**
     * Various tests for an unknown collection (group) that does not exist in TableExport Request GraphQL query.
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
        // entity "group" does not exist.
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

        //JSON API bad request, ba31ca4e-ed8f-4be0-a0f3-12088fa9263a does not exist.
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

      //GRAPHQL bad request, ba31ca4e-ed8f-4be0-a0f3-12088fa9263a does not exist.
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
                                                // Fetching Book with all fields including relationships.
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

        Response response = getJSONAPIResponse("0b0dd4e6-9cdc-4bbc-8db2-5c1491c5ee1e");
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
    }

    /**
     * Test for a GraphQL query as a Table Export Request with a Bad Export Query.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void graphQLBadExportQueryFail() throws InterruptedException {

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
                .header("sleep", "1000")
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075df828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        String responseGraphQL = getGraphQLResponse("edc4a871-dff2-4054-804e-d80075df828e");
        expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075df828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                + "\"result\":{\"message\":\"Bad Request Body'Can't parse query: { book { edges { node { title } } }'\",\"url\":null,"
                + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

        assertEquals(expectedResponse, responseGraphQL);
    }

    /**
     * Test for a GraphQL query as a Table Export Request with Multiple Queries.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void graphQLMultipleQueryFail() throws InterruptedException {

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
                .header("sleep", "1000")
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-def2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } } author { edges { node { name } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        String responseGraphQL = getGraphQLResponse("edc4a871-def2-4054-804e-d80075cf828e");
        expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-def2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                + "\"result\":{\"message\":\"Export is only supported for single Query with one root projection.\",\"url\":null,"
                + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

        assertEquals(expectedResponse, responseGraphQL);
    }

    /**
     * Test for a GraphQL query as a Table Export Request with Multiple Projections.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void graphQLMultipleProjectionFail() throws InterruptedException {

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
                .header("sleep", "1000")
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-daf2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title } } } } { author { edges { node { name } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        String responseGraphQL = getGraphQLResponse("edc4a871-daf2-4054-804e-d80075cf828e");
        expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-daf2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                + "\"result\":{\"message\":\"Export is only supported for single Query with one root projection.\",\"url\":null,"
                + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

        assertEquals(expectedResponse, responseGraphQL);
    }

    /**
     * Test for a GraphQL query as a Table Export Request with Relationship fetch.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void graphQLRelationshipFetchFail() throws InterruptedException {

        TableExport queryObj = new TableExport();
        queryObj.setId("edc4a871-dbf2-4054-804e-d80075cf828e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setResultType("CSV");
        // Fetching relationship "authors"
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
                .header("sleep", "1000")
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dbf2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { title authors {edges { node { name } } } } } } }\\\", \\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\",\"resultType\":\"CSV\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        String responseGraphQL = getGraphQLResponse("edc4a871-dbf2-4054-804e-d80075cf828e");
        expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dbf2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                + "\"result\":{\"message\":\"Export is not supported for Query that requires traversing Relationships.\",\"url\":null,"
                + "\"httpStatus\":200,\"recordCount\":null}}}]}}}";

        assertEquals(expectedResponse, responseGraphQL);
    }

    /**
     * Test for making a TableExport request to a model to which the user does not have permissions.
     * It will finish successfully but only with header.
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    @Test
    public void noReadEntityTests() throws InterruptedException, IOException {
        Resource noRead = resource(
                type("noread"),
                attributes(
                        attr("field", "No Read")
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(noRead).toJSON()
                )
                .post("/noread")
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
                                                attr("query", "/noread?fields%5Bnoread%5D=field"),
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

        Response response = getJSONAPIResponse("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e");

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
        String fileContents = getStoredFileContents(port, "0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e");

        assertEquals(1, fileContents.split("\n").length);
        assertTrue(fileContents.contains("field"));
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

        EntityDictionary dictionary = new EntityDictionary(AsyncIntegrationTestApplicationResourceConfig.MAPPINGS);
        dataStore.populateEntityDictionary(dictionary);
        DataStoreTransaction tx = dataStore.beginTransaction();
        tx.createObject(queryObj, null);
        tx.commit(null);
        tx.close();

        Elide elide = new Elide(new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(dictionary)
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

        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .header("sleep", "1000")
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

    private JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return graphqlNode;
    }

    private String getStoredFileContents(Integer port, String id) {
        return given()
                .when()
                .get("http://localhost:" + port + "/export/" + id)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();
    }
}
