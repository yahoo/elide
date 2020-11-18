/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.elide.core.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

/**
 * Dynamic Configuration functional test.
 * TODO - All of the tests in this file need to migrate over to AggregationDataStore so they are not duplicated
 * here and in standalone tests.
 */
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        statements = "CREATE TABLE PlayerStats (name varchar(255) not null,"
                + "\t\t countryId varchar(255), createdOn timestamp, updatedOn timestamp,"
                + "\t\t highScore bigint, primary key (name));"
                + "CREATE TABLE PlayerCountry (id varchar(255) not null,"
                + "\t\t isoCode varchar(255), primary key (id));"
                + "INSERT INTO PlayerStats (name,countryId,createdOn,updatedOn) VALUES\n"
                + "\t\t('SerenaWilliams','1','2000-10-10','2001-10-10');"
                + "INSERT INTO PlayerCountry (id,isoCode) VALUES\n"
                + "\t\t('2','IND');"
                + "INSERT INTO PlayerCountry (id,isoCode) VALUES\n"
                + "\t\t('1','USA');")
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
statements = "DROP TABLE PlayerStats; DROP TABLE PlayerCountry;")
public class DynamicConfigTest extends IntegrationTest {
    /**
     * This test demonstrates an example test using the JSON-API DSL.
     * @throws InterruptedException
     */

    @Test
    public void jsonApiGetTest() {
        String apiGetViewRequest = when()
                .get("/json/playerStats?filter=createdOn>=1999-01-01;createdOn<2001-01-01")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("playerStats"),
                                        id("0"),
                                        attributes(
                                                attr("countryCode", "USA"),
                                                attr("createdOn", "2000-10-10"),
                                                attr("highScore", null),
                                                attr("name", "SerenaWilliams"),
                                                attr("updatedOn", "2001-10")
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK).extract().response().asString();
        String apiGetViewExpected = "{\"data\":[{\"type\":\"playerStats\",\"id\":\"0\",\"attributes\":{\"countryCode\":\"USA\",\"createdOn\":\"2000-10-10\",\"highScore\":null,\"name\":\"SerenaWilliams\",\"updatedOn\":\"2001-10\"}}]}";
        assertEquals(apiGetViewExpected, apiGetViewRequest);
    }

    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            statements = "INSERT INTO PlayerStats (name,countryId,createdOn,updatedOn) VALUES\n"
                    + "\t\t('SaniaMirza','2','2000-10-10','2001-10-10');")
    @Test
    public void jsonApiGetMultiTest() {
        when()
                .get("/json/playerStats?filter=createdOn>=1999-01-01;createdOn<2002-01-01")
                .then()
                .body("data.id", hasItems("1"))
                .body("data.attributes.name", hasItems("SaniaMirza", "SerenaWilliams"))
                .body("data.attributes.countryCode", hasItems("USA", "IND"))
                .statusCode(HttpStatus.SC_OK);
    }


    @Test
    public void missingClientFilterTest() {
        String expectedError = "Querying playerStats requires a mandatory filter: "
                + "createdOn&gt;={{start}};createdOn&lt;{{end}}";

        when()
                .get("/json/playerStats")
                .then()
                .body("errors.detail", hasItems(expectedError))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void incompleteClientFilterTest() {
        String expectedError = "Querying playerStats requires a mandatory filter: "
                + "createdOn&gt;={{start}};createdOn&lt;{{end}}";

        when()
                .get("/json/playerStats?createdOn>=1999-01-01")
                .then()
                .body("errors.detail", hasItems(expectedError))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}
