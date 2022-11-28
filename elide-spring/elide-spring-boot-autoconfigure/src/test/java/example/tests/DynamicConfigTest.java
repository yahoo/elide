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

import com.yahoo.elide.core.exceptions.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

/**
 * Dynamic Configuration functional test.
 */
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = "classpath:db/test_init.sql",
        statements = "INSERT INTO PlayerStats (name,countryId,createdOn,updatedOn) VALUES\n"
            + "\t\t('SerenaWilliams','1','2000-10-10','2001-10-10');"
            + "INSERT INTO PlayerCountry (id,isoCode) VALUES\n"
            + "\t\t('2','IND');"
            + "INSERT INTO PlayerCountry (id,isoCode) VALUES\n"
            + "\t\t('1','USA');")
public class DynamicConfigTest extends IntegrationTest {
    /**
     * This test demonstrates an example test using the JSON-API DSL.
     * @throws InterruptedException
     */

    @Test
    public void jsonApiGetTest() {
        String apiGetViewExpected = "{\"data\":[{\"type\":\"playerStats\",\"id\":\"0\",\"attributes\":{\"countryCode\":\"USA\",\"createdOn\":\"2000-10-10\",\"highScore\":null,\"name\":\"SerenaWilliams\",\"updatedOn\":\"2001-10\"}}]}";
        when()
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
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(apiGetViewExpected));
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
}
