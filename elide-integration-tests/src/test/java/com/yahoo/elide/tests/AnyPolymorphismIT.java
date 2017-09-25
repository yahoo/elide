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
import static org.hamcrest.Matchers.equalTo;

public class AnyPolymorphismIT {
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";
    private final JsonParser jsonParser = new JsonParser();

    private final int horsepower = 102;
    private final String one = "1";

    private final String propertyPath = "/property";

    private final String tractorAsPropertyFile = "/AnyPolymorphismIT/AddTractorProperty.json";
    private final String smartphoneAsPropertyFile = "/AnyPolymorphismIT/AddSmartphoneProperty.json";


    @BeforeClass
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
        final String relationshipType = "data.relationships.myStuff.data.type";
        final String relationshipId = "data.relationships.myStuff.data.id";
        final String tractorType = "tractor";
        final String smartphoneType = "smartphone";
        final String includedType = "included[0].type";
        final String includedId = "included[0].id";
        final String includedSize = "included.size()";

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson(tractorAsPropertyFile))
                .post(propertyPath)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson(smartphoneAsPropertyFile))
                .post(propertyPath)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + "/1")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body(relationshipType, equalTo(tractorType),
                        relationshipId, equalTo(one));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + "/2")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body(relationshipType, equalTo(smartphoneType),
                        relationshipId, equalTo(one));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + "/1?include=myStuff")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body(includedType, equalTo(tractorType),
                        includedId, equalTo(one),
                        "included[0].attributes.horsepower", equalTo(horsepower),
                        includedSize, equalTo(1));


        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + "/2?include=myStuff")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body(includedType, equalTo(smartphoneType),
                        includedId, equalTo(one),
                        "included[0].attributes.type", equalTo("android"),
                        includedSize, equalTo(1));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.size()", equalTo(2));
    }

    public void testAnySubpaths() {
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson(tractorAsPropertyFile))
                .post(propertyPath)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + "?page[totals]")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("meta.page.totalRecords", equalTo(2));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + "/1/myStuff")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.horsepower", equalTo(horsepower),
                        "data.id", equalTo(one));

        //single entity so no page appropriate stuff
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + "/1/myStuff?page[totals]")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("meta", equalTo(null));
    }
}
