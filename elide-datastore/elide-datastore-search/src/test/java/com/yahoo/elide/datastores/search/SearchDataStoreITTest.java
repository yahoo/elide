/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import static com.jayway.restassured.RestAssured.given;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractApiResourceInitializer;
import org.testng.annotations.Test;

public class SearchDataStoreITTest extends AbstractApiResourceInitializer {

    public SearchDataStoreITTest() {
        super(DependencyBinder.class);
    }

    @Test
    public void getItemsCollection() {
        given()
            .contentType("application/vnd.api+json")
            .when()
            .get("/item")
            .then()
            .statusCode(HttpStatus.SC_OK);
    }
}
