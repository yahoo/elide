/*
 * Copyright 2020, Yahoo Inc.
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
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.integration.tests.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.resources.SecurityContextUser;
import com.yahoo.elide.test.graphql.EnumFieldSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;
import lombok.Data;

import java.io.IOException;
import java.security.Principal;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncIT extends AsyncApiIT {

    @Data
    private class AsyncQuery {

        private String id;
        private String query;

        @JsonSerialize(using = EnumFieldSerializer.class, as = String.class)
        private String queryType;
        private Integer asyncAfterSeconds;

        @JsonSerialize(using = EnumFieldSerializer.class, as = String.class)
        private String status;
    }

    public AsyncIT() {
        super("asyncQuery");
    }

    public String getGraphQLResponse(String id) throws InterruptedException {
        return super.getGraphQLResponse(id, "responseBody contentLength");
    }

    /**
     * Various tests for a JSONAPI query as a AsyncQuery Request with asyncAfterSeconds value set to 0.
     * Happy Path Test Scenario 1
     * @throws InterruptedException
     */
    @Test
    public void jsonApiHappyPath1() throws InterruptedException {

        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .header("sleep", "1000")
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("edc4a871-dff2-4054-804e-d80075cf830e"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "0")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED)
                .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf830e"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.status", equalTo("PROCESSING"))
                .body("data.attributes.result.contentLength", nullValue())
                .body("data.attributes.result.responseBody", nullValue())
                .body("data.attributes.result.httpStatus", nullValue());

        Response response = getJSONAPIResponse("edc4a871-dff2-4054-804e-d80075cf830e");

        // Validate AsyncQuery Response
        response
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf830e"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.attributes.result.contentLength", notNullValue())
                .body("data.attributes.result.recordCount", equalTo(3))
                .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                        + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                        + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                        + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}"))
                .body("data.attributes.result.httpStatus", equalTo(200));
    }

    /**
     * Various tests for a JSONAPI query as a AsyncQuery Request with asyncAfterSeconds value set to 7.
     * Happy Path Test Scenario 2
     * @throws InterruptedException
     */
    @Test
    public void jsonApiHappyPath2() throws InterruptedException {

        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .header("sleep", "1000")
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("edc4a871-dff2-4054-804e-d80075cf831f"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "7")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED)
                .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf831f"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.attributes.result.contentLength", notNullValue())
                .body("data.attributes.result.recordCount", equalTo(3))
                .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                        + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                        + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                        + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}"))
                .body("data.attributes.result.httpStatus", equalTo(200));

    }

    /**
     * Test for a GraphQL query as a Async Request with asyncAfterSeconds value set to 0.
     * Happy Path Test Scenario 1
     * @throws InterruptedException
     */
    @Test
    public void graphQLHappyPath1() throws InterruptedException {

        AsyncQuery queryObj = new AsyncQuery();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf828e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setQuery("{\"query\":\"{ book { edges { node { id title } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "asyncQuery",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType")
                                         )
                                 )
                         )
                 )
        ).toQuery();

        String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { id title } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\"}}]}}}";
        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("sleep", "1000")
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body(equalTo(expectedResponse));

        String responseGraphQL = getGraphQLResponse("edc4a871-dff2-4054-804e-d80075cf828e");
        expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                + "\"result\":{\"responseBody\":\"{\\\"data\\\":{\\\"book\\\":{\\\"edges\\\":[{\\\"node\\\":{\\\"id\\\":\\\"1\\\",\\\"title\\\":\\\"Ender's Game\\\"}},"
                + "{\\\"node\\\":{\\\"id\\\":\\\"2\\\",\\\"title\\\":\\\"Song of Ice and Fire\\\"}},"
                + "{\\\"node\\\":{\\\"id\\\":\\\"3\\\",\\\"title\\\":\\\"For Whom the Bell Tolls\\\"}}]}}}\","
                + "\"contentLength\":177,\"httpStatus\":200,\"recordCount\":3}}}]}}}";

        assertEquals(expectedResponse, responseGraphQL);
    }

    /**
     * Test for a GraphQL query as a Async Request with asyncAfterSeconds value set to 7.
     * Happy Path Test Scenario 2
     * @throws InterruptedException
     */
    @Test
    public void graphQLHappyPath2() throws InterruptedException {

        AsyncQuery queryObj = new AsyncQuery();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf829e");
        queryObj.setAsyncAfterSeconds(7);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
        queryObj.setQuery("{\"query\":\"{ book { edges { node { id title } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "asyncQuery",
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

        String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf829e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { id title } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\"}}]}}}";
        JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("sleep", "1000")
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body(equalTo(expectedResponse));

        expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf829e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                + "\"result\":{\"responseBody\":\"{\\\"data\\\":{\\\"book\\\":{\\\"edges\\\":[{\\\"node\\\":{\\\"id\\\":\\\"1\\\",\\\"title\\\":\\\"Ender's Game\\\"}},"
                + "{\\\"node\\\":{\\\"id\\\":\\\"2\\\",\\\"title\\\":\\\"Song of Ice and Fire\\\"}},"
                + "{\\\"node\\\":{\\\"id\\\":\\\"3\\\",\\\"title\\\":\\\"For Whom the Bell Tolls\\\"}}]}}}\","
                + "\"httpStatus\":200,\"contentLength\":177}}}]}}}";
        given()
                 .contentType(MediaType.APPLICATION_JSON)
                 .accept(MediaType.APPLICATION_JSON)
                 .body("{\"query\":\"{ asyncQuery(ids: [\\\"edc4a871-dff2-4054-804e-d80075cf829e\\\"]) "
                         + "{ edges { node { id queryType status result "
                         + "{ responseBody httpStatus contentLength } } } } }\","
                         + "\"variables\":null}")
                 .post("/graphQL")
                 .then()
                 .statusCode(org.apache.http.HttpStatus.SC_OK)
                 .body(equalTo(expectedResponse));
    }

    /**
     * Test for QueryStatus Set to PROCESSING instead of Queued.
     */
    @Test
    public void graphQLTestCreateFailOnQueryStatus() {

        AsyncQuery queryObj = new AsyncQuery();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf839e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("PROCESSING");
        queryObj.setQuery("{\"query\":\"{ book { edges { node { id title } } } }\",\"variables\":null}");
        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "asyncQuery",
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
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("sleep", "1000")
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body(containsString("errors"));
    }

    /**
     * Various tests for an unknown collection (group) that does not exist JSONAPI query as a Async Request.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiUnknownRequestTests() throws InterruptedException {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"),
                                        attributes(
                                                attr("query", "/group?sort=genre&fields%5Bgroup%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        Response response = getJSONAPIResponse("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b");

        response
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.attributes.result.contentLength", notNullValue())
                .body("data.attributes.result.responseBody", equalTo("{\"errors\":[{\"detail\":\"Unknown collection group\"}]}"))
                .body("data.attributes.result.httpStatus", equalTo(404));
    }

    /**
     * Various tests for making a Async request for AsyncQuery request that does not exist.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiBadRequestTests() throws InterruptedException {

        //JSON API bad request
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263a")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("errors[0].detail", equalTo("Unknown identifier ba31ca4e-ed8f-4be0-a0f3-12088fa9263a for asyncQuery"));

    }

    /**
     * Various tests for making a Async request for AsyncQuery request that does not exist.
     * @throws InterruptedException
     */
    @Test
    public void graphQLBadRequestTests() throws InterruptedException {

      //GRAPHQL bad request
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263a\\\"]) "
                        + "{ edges { node { id createdOn updatedOn queryType status result "
                        + "{ responseBody httpStatus contentLength } } } } }\""
                        + ",\"variables\":null}")
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.asyncQuery", nullValue())
                .body("errors[0].message", equalTo("Exception while fetching data (/asyncQuery) : Unknown identifier "
                        + "[ba31ca4e-ed8f-4be0-a0f3-12088fa9263a] for asyncQuery"));

    }

    /**
     * Various tests for making a Async request to a model to which the user does not have permissions.
     * @throws InterruptedException
     */
    @Test
    public void noReadEntityTests() throws InterruptedException {
        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e"),
                                        attributes(
                                                attr("query", "/noread"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        Response response = getJSONAPIResponse("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e");
        // Validate AsyncQuery Response
        response
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.attributes.result.recordCount", equalTo(0))
                .body("data.attributes.result.contentLength", notNullValue())
                .body("data.attributes.result.responseBody", equalTo("{\"data\":[]}"))
                .body("data.attributes.result.httpStatus", equalTo(200));
    }

    /**
     * Tests Read Permissions on AsyncQuery Model for Admin Role.
     * @throws IOException IOException
     */
    @Test
    public void asyncQueryModelAdminReadPermissions() throws IOException {

        ElideResponse response = null;
        String id = "edc4a871-dff2-4054-804e-d80075c08959";
        String query = "test-query";

        com.yahoo.elide.async.models.AsyncQuery queryObj = new com.yahoo.elide.async.models.AsyncQuery();
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setPrincipalName("owner-user");

        dataStore.populateEntityDictionary(
                        EntityDictionary.builder()
                                .checks(AsyncIntegrationTestApplicationResourceConfig.MAPPINGS)
                                .build());
        DataStoreTransaction tx = dataStore.beginTransaction();
        tx.createObject(queryObj, null);
        tx.commit(null);
        tx.close();

        Elide elide = new Elide(new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(
                                EntityDictionary.builder()
                                        .checks(AsyncIntegrationTestApplicationResourceConfig.MAPPINGS)
                                        .build())
                        .withAuditLogger(new TestAuditLogger()).build());

        elide.doScans();

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
        response = elide.get(baseUrl, "/asyncQuery/" + id, new MultivaluedHashMap<>(), ownerUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        // Principal has Admin Role
        response = elide.get(baseUrl, "/asyncQuery/" + id, new MultivaluedHashMap<>(), securityContextAdminUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        // Principal without Admin Role
        response = elide.get(baseUrl, "/asyncQuery/" + id, new MultivaluedHashMap<>(), securityContextNonAdminUser, NO_VERSION);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getResponseCode());
    }

    /**
     * Various tests for a JSONAPI query as a Async Request with asyncAfterSeconds value set to 7.
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
                                        type("asyncQuery"),
                                        id("edc4a871-dff2-4054-804e-e80075cf831f"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "70")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));

    }
}
