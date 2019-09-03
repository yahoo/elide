/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.triggers;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;
import static com.yahoo.elide.initialization.StandardTestBinder.BILLING_SERVICE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LifeCycleHookIT extends IntegrationTest {

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    @BeforeEach
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
