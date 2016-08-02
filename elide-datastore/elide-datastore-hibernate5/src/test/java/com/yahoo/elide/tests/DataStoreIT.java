/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;

import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import example.Filtered;

public class DataStoreIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
    public static void setup() {
        DataStoreTransaction tx = dataStore.beginTransaction();
        try {
            Filtered filtered = Filtered.class.newInstance();
            tx.createObject(filtered, null);
            tx.save(filtered, null);
            Filtered filtered2 = Filtered.class.newInstance();
            tx.createObject(filtered2, null);
            tx.save(filtered2, null);
            Filtered filtered3 = Filtered.class.newInstance();
            tx.createObject(filtered3, null);
            tx.save(filtered3, null);
            tx.commit(null);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFiltered() throws Exception {
        String expected = jsonParser.getJson("/ResourceIT/testFilteredFail.json");

        given().when().get("/filtered").then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }
}
