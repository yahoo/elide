/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Relation.TO_ONE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.utils.JsonParser;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AnyPolymorphismIT extends IntegrationTest {
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";
    private final JsonParser jsonParser = new JsonParser();

    private static final Resource TRACTOR_PROPERTY = resource(
            type("property"),
            id("1"),
            attributes(),
            relationships(
                    relation("myStuff", TO_ONE,
                            linkage(type("tractor"), id("1"))
                    )
            )
    );

    @BeforeEach
    public void setUp() {
        //Create a tractor
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\": {\"type\": \"tractor\", \"attributes\": {\"horsepower\": 102 }}}")
                .post("/tractor")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        //Create a smartphone
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\": {\"type\": \"smartphone\", \"attributes\": {\"type\": \"android\" }}}")
                .post("/smartphone")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void testAny() {
        String id1 = RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(TRACTOR_PROPERTY))
                .post("/property")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("data.id");

        String id2 = RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("property"),
                                        attributes(),
                                        relationships(
                                                relation("myStuff", TO_ONE,
                                                        linkage(type("smartphone"), id("1"))
                                                )
                                        )
                                )
                        )
                )
                .post("/property")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("data.id");

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/" + id1)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.relationships.myStuff.data.type", equalTo("tractor"),
                        "data.relationships.myStuff.data.id", equalTo("1"));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/" + id2)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.relationships.myStuff.data.type", equalTo("smartphone"),
                        "data.relationships.myStuff.data.id", equalTo("1"));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/" + id1 + "?include=myStuff")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("included[0].type", equalTo("tractor"),
                        "included[0].id", equalTo("1"),
                        "included[0].attributes.horsepower", equalTo(102),
                        "included.size()", equalTo(1));


        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/" + id2 + "?include=myStuff")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("included[0].type", equalTo("smartphone"),
                        "included[0].id", equalTo("1"),
                        "included[0].attributes.type", equalTo("android"),
                        "included.size()", equalTo(1),
                        "included[0].attributes.operatingSystem", equalTo("some dessert"));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.size()", equalTo(2));
    }

    @Test
    public void testAnyupdate() {
        String id = RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(TRACTOR_PROPERTY))
                .post("/property")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.relationships.myStuff.data.type", equalTo("tractor"))
                .extract()
                .path("data.id");

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\": {\"id\": \"" + id + "\", \"type\": \"property\", \"relationships\": {\"myStuff\": {\"data\": {\"type\": \"smartphone\", \"id\": \"1\"}}}}}")
                .patch("/property/" + id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/" + id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.relationships.myStuff.data.type", equalTo("smartphone"));

        //delete relation
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\": {\"id\": \"" + id + "\", \"type\": \"property\", \"relationships\": {\"myStuff\": {\"data\": null}}}}")
                .patch("/property/" + id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/" + id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.relationships.myStuff.data", equalTo(null));
    }

    @Test
    public void testAnySubpaths() {
        String id = RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(TRACTOR_PROPERTY))
                .post("/property")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("data.id");

        Response response = RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property?page[totals]")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .response();

        Integer collectionSize = response.path("data.size()");
        Integer totalSize = response.path("meta.page.totalRecords");
        assertEquals(totalSize, collectionSize);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/" + id + "/myStuff")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.horsepower", equalTo(102),
                        "data.id", equalTo("1"));

        //show that you can't get a relation by the interface type
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/device")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("errors[0]", containsString("Unknown collection"));

        //Filtering is not supported for these types.
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/" + id + "?filter[tractor]=horsepower==103")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}
