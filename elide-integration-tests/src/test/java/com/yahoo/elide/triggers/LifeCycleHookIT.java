/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.triggers;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.initialization.LifeCycleIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.resources.JsonApiEndpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LifeCycleHookIT extends IntegrationTest {

    public LifeCycleHookIT() {
        super(LifeCycleIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @Test
    public void testBillingServiceInvocation() {

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("customerInvoice"),
                                        id("123"),
                                        attributes(
                                                attr("complete", true),
                                                attr("total", 1000)
                                        )
                                )
                        )
                )
                .post("/customerInvoice")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"))
                .body("data.attributes.total", equalTo(1100))
                .body("data.attributes.complete", equalTo(true));
    }
}
