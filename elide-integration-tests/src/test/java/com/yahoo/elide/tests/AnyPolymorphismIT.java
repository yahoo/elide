/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.jayway.restassured.RestAssured;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractApiResourceInitializer;
import com.yahoo.elide.utils.JsonParser;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class AnyPolymorphismIT extends AbstractApiResourceInitializer {
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";
    private final JsonParser jsonParser = new JsonParser();

    private final String sep = "/";

    private final int horsepower = 102;
    private final String one = "1";
    private final String tractorType = "tractor";
    private final String smartphoneType = "smartphone";

    private final String relationshipType = "data.relationships.myStuff.data.type";
    private final String relationshipId = "data.relationships.myStuff.data.id";
    private final String idPath = "data.id";

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
        final String includedType = "included[0].type";
        final String includedId = "included[0].id";
        final String includedSize = "included.size()";

        String id1 = RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson(tractorAsPropertyFile))
                .post(propertyPath)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path(idPath);

        String id2 = RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson(smartphoneAsPropertyFile))
                .post(propertyPath)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path(idPath);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + sep + id1)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body(relationshipType, equalTo(tractorType),
                        relationshipId, equalTo(one));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + sep + id2)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body(relationshipType, equalTo(smartphoneType),
                        relationshipId, equalTo(one));

        String query = "?include=myStuff";

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + sep + id1 + query)
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
                .get(propertyPath + sep + id2 + query)
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

    @Test
    public void testAnyupdate() {
        String id = RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson(tractorAsPropertyFile))
                .post(propertyPath)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body(relationshipType, equalTo(tractorType))
                .extract()
                .path(idPath);

        String prefix = "{\"data\": {\"id\": \"" + id;

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(prefix + "\", \"type\": \"property\", \"relationships\": {\"myStuff\": {\"data\": {\"type\": \"smartphone\", \"id\": \"1\"}}}}}")
                .patch(propertyPath + sep + id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + sep + id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body(relationshipType, equalTo(smartphoneType));

        //delete relation
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(prefix + "\", \"type\": \"property\", \"relationships\": {\"myStuff\": {\"data\": null}}}}")
                .patch(propertyPath + sep + id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + sep + id)
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
                .body(jsonParser.getJson(tractorAsPropertyFile))
                .post(propertyPath)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path(idPath);

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + "?page[totals]")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("meta.page.totalRecords", greaterThan(0));

        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + sep + id + "/myStuff")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.horsepower", equalTo(horsepower),
                        idPath, equalTo(one));

        //single entity so no page appropriate stuff
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + sep + id + "/myStuff?page[totals]")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("meta", equalTo(null));

        //Filtering is not supported for these types.
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get(propertyPath + sep + id + "?filter[tractor]=horsepower==103")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}
