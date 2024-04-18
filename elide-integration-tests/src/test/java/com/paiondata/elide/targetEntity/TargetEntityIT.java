/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.targetEntity;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import org.junit.jupiter.api.Test;

public class TargetEntityIT extends IntegrationTest {

    @Test
    public void testEmployeeHierarchy() {
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("swe"),
                                        attributes(
                                                attr("name", "peon")
                                        )
                                )
                        )
                )
                .post("/swe")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"));

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("boss"),
                                        attributes(
                                                attr("name", "boss")
                                        ),
                                        relationships(
                                                relation("reports", linkage(type("swe"), id("1")))
                                        )
                                )
                        )
                )
                .post("/boss")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"));

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .when()
                .get("/boss/1")
                .then()
                .log().all()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.id", equalTo("1"),
                        "data.relationships.reports.data.id", contains("1"),
                        "data.relationships.reports.data.type", contains("swe")
                );

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .when()
                .get("/swe/1")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.id", equalTo("1"),
                        "data.relationships.boss.data.id", equalTo("1"),
                        "data.relationships.boss.data.type", equalTo("boss")
                );
    }
}
