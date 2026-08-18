/*
 * Copyright 2019, Yahoo Inc.
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
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.exceptions.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Example functional tests for Aggregation Store.
 */
@Sql(
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = "classpath:db/test_init.sql",
        statements = {
                "INSERT INTO Stats (id, measure, dimension) VALUES\n"
                    + "\t\t(1,100,'Foo'),"
                    + "\t\t(2,150,'Bar');"
})
public class AggregationStoreTest extends IntegrationTest {

    /**
     * This test demonstrates an example test using the aggregation store.
     */
    @Test
    public void jsonApiGetTestNoHeader(@Autowired MeterRegistry metrics) {
        when()
                .get("/json/stats?fields[stats]=measure")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("stats"),
                                        id("0"),
                                        attributes(
                                                attr("measure", 250)
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);
        when()
        .get("/json/stats?fields[stats]=measure")
        .then()
        .body(equalTo(
                data(
                        resource(
                                type("stats"),
                                id("0"),
                                attributes(
                                        attr("measure", 250)
                                )
                        )
                ).toJSON())
        )
        .statusCode(HttpStatus.SC_OK);
        assertTrue(metrics
                .get("cache.gets")
                .tags("cache", "elideQueryCache", "result", "hit")
                .functionCounter().count() > 0);
    }

    /**
     * This test demonstrates an example test using the aggregation store.
     */
    @Test
    public void jsonApiGetTest(@Autowired MeterRegistry metrics) {
        Map<String, String> requestHeaders = new HashMap<>();
         requestHeaders.put("bypassCache", "true");
         HttpHeaders headers = new HttpHeaders();
         headers.set("bypassCache", "true");
         given().headers(headers)
                .get("/json/stats?fields[stats]=measure")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("stats"),
                                        id("0"),
                                        attributes(
                                                attr("measure", 250)
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);

         given().headers(requestHeaders)
         .get("/json/stats?fields[stats]=measure")
         .then()
         .body(equalTo(
                 data(
                         resource(
                                 type("stats"),
                                 id("0"),
                                 attributes(
                                         attr("measure", 250)
                                 )
                         )
                 ).toJSON())
         )
         .statusCode(HttpStatus.SC_OK);
        assertFalse(metrics
                .get("cache.gets")
                .tags("cache", "elideQueryCache", "result", "hit")
                .functionCounter().count() > 0);
    }

    @Test
    public void metaDataTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/json/namespace/default") //"default" namespace added by Agg Store
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("default"))
                .body("data.attributes.friendlyName", equalTo("default"));
    }
}
