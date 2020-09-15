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
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.HttpStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Example functional tests for Aggregation Store.
 */
@Import(DBPasswordExtractorSetup.class)
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
    public void jsonApiGetTest(@Autowired MeterRegistry metrics) {
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

        // query cache was active and publishing metrics
        assertEquals(
                1,
                metrics.get("cache.gets").tags("cache", "elideQueryCache", "result", "miss").functionCounter().count()
        );
    }

    /**
     * This test demonstrates an example test using the aggregation store from dynamic configuration.
     */
    @Test
    public void testDynamicAggregationModel() {

        String getPath = "/json/orderDetails?sort=customerRegion,orderMonth&"
                        + "fields[orderDetails]=orderTotal,customerRegion,orderMonth&filter=orderMonth>=2020-08";

        when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data", hasSize(3))
            .body("data.id", hasItems("0", "1", "2"))
            .body("data.attributes", hasItems(
                            allOf(hasEntry("customerRegion", "NewYork"), hasEntry("orderMonth", "2020-08")),
                            allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderMonth", "2020-08")),
                            allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderMonth", "2020-09"))))
            .body("data.attributes.orderTotal", hasItems(61.43F, 113.07F, 260.34F));
    }
}
