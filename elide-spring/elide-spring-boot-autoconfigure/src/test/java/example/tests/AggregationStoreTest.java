/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.HttpStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Example functional tests for Aggregation Store.
 */
public class AggregationStoreTest extends IntegrationTest {
    /**
     * This test demonstrates an example test using the aggregation store.
     */
    @Test
    @Sql(statements = {
            "DROP TABLE Stats IF EXISTS;",
            "CREATE TABLE Stats(id int, measure int, dimension VARCHAR(255));",
            "INSERT INTO Stats (id, measure, dimension) VALUES\n"
                    + "\t\t(1,100,'Foo'),"
                    + "\t\t(2,150,'Bar');"
    })
    public void jsonApiGetTestNoCache(@Autowired MeterRegistry metrics) {

        given()
        .headers("bypassingCache", "true")
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

        // query cache inactive by default
        System.out.println(metrics.toString());
        System.out.println(metrics.get("cache.gets").toString());
        System.out.println(" ************* " + metrics
                .get("cache.gets")
                .tags("cache", "elideQueryCache", "result", "miss")
                .functionCounter().count());
        assertFalse(metrics
                .get("cache.gets")
                .tags("cache", "elideQueryCache", "result", "miss")
                .functionCounter().count() > 0);
    }
    public void jsonApiGetTestCache(@Autowired MeterRegistry metrics) {

        given()
        .headers("bypassingCache", "true")
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

        // query cache inactive by default
        System.out.println(metrics.toString());
        System.out.println(metrics.get("cache.gets").toString());
        System.out.println(" ************* " + metrics
                .get("cache.gets")
                .tags("cache", "elideQueryCache", "result", "miss")
                .functionCounter().count());
        assertTrue(metrics
                .get("cache.gets")
                .tags("cache", "elideQueryCache", "result", "miss")
                .functionCounter().count() > 0);
    }
}
