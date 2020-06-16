/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.query;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL;
import com.yahoo.elide.core.HttpStatus;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

/**
 * Example functional test.
 */
public class ExampleTest extends IntegrationTest {

    /**
     * This test demonstrates an example test using the JSON-API DSL.
     */
    @Test
    void jsonApiTest() {
        when()
                .get("/api/v1/user")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type( "user"),
                                        id("1"),
                                        attributes(
                                                attr("name", "Jon Doe"),
                                                attr("role", "Registered")
                                        )
                                ),
                                resource(
                                        type( "user"),
                                        id("2"),
                                        attributes(
                                                attr("name", "Jane Doe"),
                                                attr("role", "Registered")


                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * This test demonstrates an example test using the GraphQL DSL.
     */
    @Test
    void graphqlTest() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body("{ \"query\" : \"" + GraphQLDSL.document(
                query(
                    selection(
                        field("user",
                            selections(
                                field("id"),
                                field("name"),
                                field("role")
                            )
                        )
                    )
                )
            ).toQuery() + "\" }"
        )
        .when()
            .post("/graphql/api/v1")
            .then()
            .body(equalTo(GraphQLDSL.document(
                selection(
                    field(
                        "user",
                        selections(
                            field("id", "1"),
                            field( "name", "Jon Doe"),
                            field("role", "Registered")
                        ),
                        selections(
                            field("id", "2"),
                            field( "name", "Jane Doe"),
                            field("role", "Registered")
                        )
                    )
                )
            ).toResponse()))
            .statusCode(HttpStatus.SC_OK);
    }
}
