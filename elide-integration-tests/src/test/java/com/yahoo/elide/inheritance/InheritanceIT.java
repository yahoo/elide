/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.inheritance;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.mutation;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.yahoo.elide.contrib.testhelpers.graphql.VariableFieldSerializer;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.GraphQLTestUtils;
import com.yahoo.elide.initialization.IntegrationTest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
@Tag("skipInMemory")  //In memory store doesn't support inheritance.
public class InheritanceIT extends IntegrationTest {

    private GraphQLTestUtils testUtils = new GraphQLTestUtils();

    @Data
    private static class Droid {
        @JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
        private String name;

        @JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
        private String primaryFunction;
    }

    @BeforeEach
    public void createCharacters() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("droid"),
                                        id("C3P0"),
                                        attributes(
                                                attr("primaryFunction", "protocol droid")
                                        )
                                )
                        )
                )
                .post("/droid")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("C3P0"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("hero"),
                                        id("Luke Skywalker"),
                                        attributes(
                                                attr("forceSensitive", true)
                                        )
                                )
                        )
                )
                .post("/hero")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("Luke Skywalker"));
    }

    @Test
    public void testEmployeeHierarchy() {

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("manager"),
                                        id(null)
                                )
                        )
                )
                .post("/manager")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("employee"),
                                        id(null),
                                        attributes(),
                                        relationships(
                                                relation("boss",
                                                        linkage(type("manager"), id("1"))
                                                )
                                        )
                                )
                        )
                )
                .post("/manager/1/minions")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"),
                        "data.relationships.boss.data.id", equalTo("1")
                );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/manager/1")
                .then()
                .statusCode(SC_OK)
                .body("data.id", equalTo("1"),
                        "data.relationships.minions.data.id", contains("1"),
                        "data.relationships.minions.data.type", contains("employee")
                );
    }

    @Test
    public void testJsonApiCharacterHierarchy() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/character")
                .then()
                .statusCode(SC_OK)
                .body(equalTo(data(
                        resource(
                                type("droid"),
                                id("C3P0"),
                                attributes(
                                        attr("primaryFunction", "protocol droid")
                                )
                        ),
                        resource(
                                type("hero"),
                                id("Luke Skywalker"),
                                attributes(
                                        attr("forceSensitive", true)
                                )
                        )
                ).toJSON()));
    }

    @Test
    public void testGraphQLCharacterHierarchy() throws Exception {

        String query = document(
                selection(
                        field(
                                "character",
                                selections(
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "character",
                                selections(
                                        field("name", "C3P0")
                                ),
                                selections(
                                        field("name", "Luke Skywalker")
                                )
                        )
                )
        ).toResponse();

        testUtils.runQueryWithExpectedResult(query, expected);
    }

    @Test
    public void testGraphQLDroidFragment() throws Exception {

        String query = "{ character { edges { node { "
                + "__typename ... on Character { name } "
                + "__typename ... on Droid { primaryFunction }}}}}";

        String expected = document(
                selections(
                        field(
                                "character",
                                selections(
                                        field("__typename", "Droid"),
                                        field("name", "C3P0"),
                                        field("primaryFunction", "protocol droid")
                                ),
                                selections(
                                        field("__typename", "Hero"),
                                        field("name", "Luke Skywalker")
                                )
                        )
                )
        ).toResponse();

        testUtils.runQueryWithExpectedResult(query, expected);
    }

    @Test
    public void testGraphQLInvalidFragment() throws Exception {

        String query = "{ character { edges { node { "
                + "__typename ... on Manager { id } "
                + "__typename ... on Droid { primaryFunction }}}}}";

        testUtils.runQuery(query, new HashMap<>())
                .statusCode(SC_OK)
                .body(startsWith("{\"errors\":[{\"message\":\"Validation error of type InvalidFragmentType: "));
    }


    @Test
    public void testGraphQLCharacterUpsertFailure() throws Exception {
        Droid droid = new Droid();
        droid.setName("IG-88");

        String query = document(
                mutation(
                    selection(
                            field(
                                    "character",
                                    arguments(
                                            argument("op", "UPSERT"),
                                            argument("data", droid)
                                    ),
                                    selections(
                                        field("name")
                                    )
                            )
                    )
                )
            ).toQuery();

        String expected = "{\"data\":null,\"errors\":[{\"message\":\"Exception while fetching data (/character) "
                + ": Bad Request Body&#39;Cannot create an entity model of "
                + "type: Character&#39;\",\"locations\":[{\"line\":1,\"column\":11}],\"path\":[\"character\"]}]}";
        testUtils.runQueryWithExpectedResult(query, expected);
    }

    @Test
    public void testGraphQLCharacterInvalidFilter() throws Exception {

        String query = document(
                selection(
                        field(
                                "character",
                                arguments(
                                        argument("op", "FETCH"),
                                        argument("filter", "primaryFunction==*protocol*", true)
                                ),
                                selections(
                                        field("name")
                                )
                        )
                )).toQuery();

        String expected = "{\"errors\":[{\"message\":\"Invalid filter format: filter\\nNo such association "
                + "primaryFunction for type character\\nInvalid filter format: filter[Optional[character]]\\nInvalid "
                + "query parameter: filter[Optional[character]]\"}]}";

        testUtils.runQueryWithExpectedResult(query, expected);
    }

    @Test
    public void testJsonApiCharacterInvalidFilter() {
        String expected = "{\"errors\":[{\"detail\":\"Invalid filter format: filter\\nNo such association "
                + "primaryFunction for type character\\nInvalid filter format: "
                + "filter\\nInvalid query parameter: filter\"}]}";

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/character?filter=primaryFunction==*droid*")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void testJsonApiCharacterHierarchySparseFields() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/character?fields[droid]=primaryFunction&fields[hero]=id")
                .then()
                .statusCode(SC_OK)
                .body(equalTo(data(
                        resource(
                                type("droid"),
                                id("C3P0"),
                                attributes(
                                        attr("primaryFunction", "protocol droid")
                                )
                        ),
                        resource(
                                type("hero"),
                                id("Luke Skywalker")
                        )
                ).toJSON()));
    }

    @Test
    public void testJsonApiCharacterInvalidCreation() {
        String expected = "{\"errors\":[{\"detail\":\"Bad Request Body&#39;Cannot create an "
                + "entity model of type: Character&#39;\"}]}";

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("character"),
                                        id("IG-88"),
                                        attributes(
                                                attr("primaryFunction", "assassin droid")
                                        )
                                )
                        )
                )
                .post("/character")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void testGraphQLCharacterInvalidSort() throws Exception {

        String query = document(
                selection(
                        field(
                                "character",
                                arguments(
                                        argument("op", "FETCH"),
                                        argument("sort", "-primaryFunction", true)
                                ),
                                selections(
                                        field("name")
                                )
                        )
                )).toQuery();

        String expected = "{\"errors\":[{\"message\":\"Invalid sorting clause -primaryFunction for type character\"}]}";

        testUtils.runQueryWithExpectedResult(query, expected);
    }

    @Test
    public void testJsonApiCharacterInvalidSort() {
        String expected = "{\"errors\":[{\"detail\":\"Invalid value: character does "
                + "not contain the field primaryFunction\"}]}";

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/character?sort=-primaryFunction")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }
}
