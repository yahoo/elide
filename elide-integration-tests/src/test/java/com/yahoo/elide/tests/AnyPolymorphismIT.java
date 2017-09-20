/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.jayway.restassured.RestAssured;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.utils.JsonParser;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.hamcrest.Matchers.*;

public class AnyPolymorphismIT {
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
    public void setup() {
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
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/AnyPolymorphismIT/AddTractorProperty.json"))
                .post("/property")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/AnyPolymorphismIT/AddSmartphoneProperty.json"))
                .post("/property")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/1")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.relationships.myStuff.data.type", equalTo("tractor"),
                        "data.relationships.myStuff.data.id", equalTo("1"));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/2")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.relationships.myStuff.data.type", equalTo("smartphone"),
                        "data.relationships.myStuff.data.id", equalTo("1"));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/property/1?include=myStuff")
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
                .get("/property/2?include=myStuff")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("included[0].type", equalTo("smartphone"),
                        "included[0].id", equalTo("1"),
                        "included[0].attributes.type", equalTo("android"),
                        "included.size()", equalTo(1));
    }
}
