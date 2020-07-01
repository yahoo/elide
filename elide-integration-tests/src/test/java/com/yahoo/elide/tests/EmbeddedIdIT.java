/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.tests;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import example.embeddedid.Address;
import example.embeddedid.Building;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class EmbeddedIdIT extends IntegrationTest {

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(new EntityDictionary(new HashMap<>()));
        DataStoreTransaction tx = dataStore.beginTransaction();

        Address address = new Address();
        address.setStreet("Bullion Blvd");
        address.setZipCode(40121);

        Building building = new Building();
        building.setAddress(address);
        building.setName("Fort Knox");

        tx.createObject(building, null);

        tx.commit(null);
        tx.close();
    }

    @Test
    public void testFetchCollection() {
        given()
                .when()
                .get("/building")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testFetchById() {
        given()
                .when()
                .get("/building/QWRkcmVzcyhudW1iZXI9MCwgc3RyZWV0PUJ1bGxpb24gQmx2ZCwgemlwQ29kZT00MDEyMSk=")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testFilterById() {
        given()
                .when()
                .get("/building?filter=id=='QWRkcmVzcyhudW1iZXI9MCwgc3RyZWV0PUJ1bGxpb24gQmx2ZCwgemlwQ29kZT00MDEyMSk='")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }
}
