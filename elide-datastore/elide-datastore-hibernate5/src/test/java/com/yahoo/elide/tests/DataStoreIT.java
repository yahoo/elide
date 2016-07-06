/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;
import example.Filtered;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class DataStoreIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
    public static void setup() {
        DataStoreTransaction tx = dataStore.beginTransaction();

        tx.save(tx.createObject(Filtered.class));
        tx.save(tx.createObject(Filtered.class));
        tx.save(tx.createObject(Filtered.class));

        tx.commit();
    }

    @Test
    public void testFiltered() throws Exception {
        String expected = jsonParser.getJson("/ResourceIT/testFiltered.json");

        given().when().get("/filtered").then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }
}
