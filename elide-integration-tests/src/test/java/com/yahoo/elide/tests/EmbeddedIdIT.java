/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import example.embeddedid.Address;
import example.embeddedid.AddressSerde;
import example.embeddedid.Building;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmbeddedIdIT extends IntegrationTest {

    protected Address address1 = new Address(0, "Bullion Blvd", 40121);
    protected Address address2 = new Address(1409, "W Green St", 61801);
    protected AddressSerde serde = new AddressSerde();

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(new EntityDictionary(new HashMap<>()));
        DataStoreTransaction tx = dataStore.beginTransaction();

        Building building = new Building();
        building.setAddress(address1);
        building.setName("Fort Knox");

        tx.createObject(building, null);

        tx.commit(null);
        tx.close();
    }

    @Test
    public void testJsonApiFetchCollection() {
        given()
                .when()
                .get("/building")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testJsonApiFetchById() {
        given()
                .when()
                .get("/building/" + serde.serialize(address1))
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testJsonApiFilterById() {
        given()
                .when()
            .get("/building?filter=id=='" + serde.serialize(address1) + "'")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testJsonApiCreate() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(data(
                        resource(
                                type("building"),
                                id(serde.serialize(address2)),
                                attributes(attr(
                                        "name", "Altegeld Hall"
                                ))
                        )

                ))
                .when()
                .post("/building")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(
                        resource(
                            type("building"),
                            id(serde.serialize(address2)),
                            attributes(attr(
                                    "name", "Altegeld Hall"
                            ))
                        )).toJSON()
                ));
    }


    @Test
    public void testGraphQLFetchCollection() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "building",
                                selections(
                                        field("address"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "building",
                                selections(
                                        field("address", serde.serialize(address1)),
                                        field("name", "Fort Knox")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLFetchById() throws Exception {
        String addressId = serde.serialize(address1);

        String graphQLRequest = document(
                selection(
                        field(
                                "building",
                                arguments(
                                        argument("ids", Arrays.asList(addressId))
                                ),
                                selections(
                                        field("address"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "building",
                                selections(
                                        field("address", addressId),
                                        field("name", "Fort Knox")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLFilterById() throws Exception {
        String addressId = serde.serialize(address1);

        String graphQLRequest = document(
                selection(
                        field(
                                "building",
                                arguments(
                                        argument("filter", "\"id==\\\"" + addressId + "\\\"\"")
                                ),
                                selections(
                                        field("address"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "building",
                                selections(
                                        field("address", addressId),
                                        field("name", "Fort Knox")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    private void create(String query, Map<String, Object> variables) throws IOException {
        runQuery(toJsonQuery(query, variables));
    }

    private void runQueryWithExpectedResult(
            String graphQLQuery,
            Map<String, Object> variables,
            String expected
    ) throws IOException {
        compareJsonObject(runQuery(graphQLQuery, variables), expected);
    }

    private void runQueryWithExpectedResult(String graphQLQuery, String expected) throws IOException {
        runQueryWithExpectedResult(graphQLQuery, null, expected);
    }

    private void compareJsonObject(ValidatableResponse response, String expected) throws IOException {
        JsonNode responseNode = mapper.readTree(response.extract().body().asString());
        JsonNode expectedNode = mapper.readTree(expected);
        assertEquals(expectedNode, responseNode);
    }

    private ValidatableResponse runQuery(String query, Map<String, Object> variables) throws IOException {
        return runQuery(toJsonQuery(query, variables));
    }

    private ValidatableResponse runQuery(String query) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .post("/graphQL")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    private String toJsonArray(JsonNode... nodes) throws IOException {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode node : nodes) {
            arrayNode.add(node);
        }
        return mapper.writeValueAsString(arrayNode);
    }

    private String toJsonQuery(String query, Map<String, Object> variables) throws IOException {
        return mapper.writeValueAsString(toJsonNode(query, variables));
    }

    private JsonNode toJsonNode(String query) {
        return toJsonNode(query, null);
    }

    private JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return graphqlNode;
    }
}
