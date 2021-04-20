/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.initialization.GraphQLIntegrationTest;
import example.embeddedid.*;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.graphql.GraphQLDSL.document;
import static com.yahoo.elide.test.graphql.GraphQLDSL.*;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class EmbeddedFilterIT extends GraphQLIntegrationTest {

    protected HouseAddress address1 = new HouseAddress(1000, "Bullion Blvd", 40121);
    protected HouseAddress address2 = new HouseAddress(1409, "W Green St", 61801);
    protected HouseAddress address3 = new HouseAddress(1800, "South First Street", 61820);

    private static final String ATTRIBUTES = "attributes";
    private static final String RELATIONSHIPS = "relationships";
    private static final String INCLUDED = "included";

    protected final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    public void beforeAll() {

    }

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(new EntityDictionary(new HashMap<>()));
        DataStoreTransaction tx = dataStore.beginTransaction();

        House house1 = new House();
        house1.setAddress(address1);
        house1.setName("Lannister");

        House house2 = new House();
        house2.setAddress(address2);
        house2.setName("Stark");

        House house3 = new House();
        house3.setAddress(address3);
        house3.setName("Baratheon");

        tx.createObject(house1, null);
        tx.createObject(house2, null);
        tx.createObject(house3, null);

        tx.commit(null);
        tx.close();
    }

    @Test
    public void testJsonApiFilterByNumber() throws JsonProcessingException {
        JsonNode responseBody = mapper.readTree(
                given()
                        .when()
                        .get("/house?filter=address.number=='1409'")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        System.out.println(responseBody.get("data"));

        for (JsonNode houseNode : responseBody.get("data")) {
            assertTrue(houseNode.has(ATTRIBUTES));

            JsonNode attributes = houseNode.get(ATTRIBUTES);
            assertEquals(2, attributes.size());
            assertTrue(attributes.has("address"));
        }
    }

    @Test
    public void testJsonApiFilterByInClause() throws JsonProcessingException {
        JsonNode responseBody = mapper.readTree(
                given()
                        .when()
                        .get("/house?filter=address.number=in='1409'")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        System.out.println(responseBody.get("data"));

        for (JsonNode houseNode : responseBody.get("data")) {
            assertTrue(houseNode.has(ATTRIBUTES));

            JsonNode attributes = houseNode.get(ATTRIBUTES);
            assertEquals(2, attributes.size());
            assertTrue(attributes.has("address"));
        }
    }

}
