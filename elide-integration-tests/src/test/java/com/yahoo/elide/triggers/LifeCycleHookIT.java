/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.triggers;

import static com.jayway.restassured.RestAssured.given;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static com.yahoo.elide.initialization.StandardTestBinder.BILLING_SERVICE;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;

import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LifeCycleHookIT extends AbstractIntegrationTestInitializer {

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    @BeforeTest
    public void resetMocks() {
        reset(BILLING_SERVICE);
    }

    @Test
    public void testBillingServiceInvocation() {

        doReturn(100L).when(BILLING_SERVICE).purchase(ArgumentMatchers.any());

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
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
                .log().all()
                .post("/customerInvoice")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"))
                .body("data.attributes.total", equalTo(1100))
                .body("data.attributes.complete", equalTo(true));
    }
}
