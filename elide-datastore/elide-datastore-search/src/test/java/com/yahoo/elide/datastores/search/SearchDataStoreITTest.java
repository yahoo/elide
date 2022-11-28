/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.AbstractApiResourceInitializer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SearchDataStoreITTest extends AbstractApiResourceInitializer {

    public SearchDataStoreITTest() {
        super(DependencyBinder.class);
    }

    @Test
    public void getEscapedItem() {
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .when()
            .get("/item?filter[item]=name==*-luc*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", equalTo(Arrays.asList("6")));
    }

    @Test
    public void testObjectIndexing() {
       /* Add a new item */
       given()
           .contentType(JSONAPI_CONTENT_TYPE)
           .body(
                   data(
                       resource(
                          type("item"),
                          id(1000),
                          attributes(
                                  attr("name", "Another Drum"),
                                  attr("description", "Onyx Timpani Drum")
                          )
                       )
                   ).toJSON())
           .when()
           .post("/item")
           .then()
           .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        /* This query hits the index */
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .when()
            .get("/item?filter[item]=name=ini=*DrU*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "3", "1000"));

        /* This query hits the DB */
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .when()
            .get("/item")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "2", "3", "4", "5", "6", "7", "1000"));

        /* Delete the newly added item */
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .when()
            .delete("/item/1000")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        /* This query hits the index */
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .when()
            .get("/item?filter[item]=name==*dru*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "3"));

        /* This query hits the DB */
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .when()
            .get("/item")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "2", "3", "4", "5", "6", "7"));
    }
}
