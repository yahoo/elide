/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.tests;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;


import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.spring.controllers.JsonApiController;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

/**
 * Dynamic Configuration functional test.
 */
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        statements = "INSERT INTO PlayerStats (name,countryId,createdOn) VALUES\n"
                + "\t\t('SerenaWilliams','1','2000-10-01');"
                + "INSERT INTO PlayerCountry (id,isoCode) VALUES\n"
                + "\t\t('2','IND');"
                + "INSERT INTO PlayerCountry (id,isoCode) VALUES\n"
                + "\t\t('1','USA');")
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
statements = "DELETE FROM PlayerStats; DELETE FROM PlayerCountry;")
public class DynamicConfigTest extends IntegrationTest {
    /**
     * This test demonstrates an example test using the JSON-API DSL.
     * @throws InterruptedException
     */

    @Test
    public void jsonApiGetViewTest() {
        String apiGetViewRequest = when()
                .get("/json/playerStatsView")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("playerStatsView"),
                                        id("0"),
                                        attributes(
                                                attr("countryCode", "USA"),
                                                attr("createdOn", "2000-10-01T04:00Z"),
                                                attr("highScore", null),
                                                attr("name", "SerenaWilliams")
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK).extract().response().asString();
        String apiGetViewExpected = "{\"data\":[{\"type\":\"playerStatsView\",\"id\":\"0\",\"attributes\":{\"countryCode\":\"USA\",\"createdOn\":\"2000-10-01T04:00Z\",\"highScore\":null,\"name\":\"SerenaWilliams\"}}]}";
        assertEquals(apiGetViewRequest, apiGetViewExpected);
    }

    @Test
    public void jsonApiPostViewTest() {
        String apiPostViewRequest = given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("playerStats"),
                                        id("SaniaMirza"),
                                        attributes(
                                                attr("countryId", "1"),
                                                attr("createdOn", "2002-03-01T04:00Z"),
                                                attr("highScore", null)
                                        )
                                )
                        )
                )
                .when()
                .post("/json/playerStats")
                .then()
                .body(equalTo(datum(
                        resource(
                                type("playerStats"),
                                id("SaniaMirza"),
                                attributes(
                                        attr("countryId", "1"),
                                        attr("createdOn", "2002-03-01T04:00Z"),
                                        attr("highScore", null)
                                )
                        )
                ).toJSON()))
                .statusCode(HttpStatus.SC_CREATED).extract().response().asString();
        String apiPostViewExpected = "{\"data\":{\"type\":\"playerStats\",\"id\":\"SaniaMirza\",\"attributes\":{\"countryId\":\"1\",\"createdOn\":\"2002-03-01T04:00Z\",\"highScore\":null}}}";
        assertEquals(apiPostViewRequest, apiPostViewExpected);
    }
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            statements = "INSERT INTO PlayerStats (name,countryId,createdOn) VALUES\n"
                    + "\t\t('SaniaMirza','2','2000-10-01');")
    @Test
    public void jsonApiPostGetViewTest() {
        when()
                .get("/json/playerStatsView")
                .then()
                .body("data.id", hasItems("1"))
                .body("data.attributes.name", hasItems("SaniaMirza", "SerenaWilliams"))
                .body("data.attributes.countryCode", hasItems("USA", "IND"))
                .statusCode(HttpStatus.SC_OK);
    }
}
