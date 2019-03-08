/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import com.yahoo.elide.standalone.models.Post;

import org.apache.http.HttpStatus;
import org.glassfish.hk2.api.ServiceLocator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Tests ElideStandalone starts and works
 */
public class ElideStandaloneTest {
    private ElideStandalone elide;

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    @BeforeClass
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneSettings() {

            @Override
            public ElideSettings getElideSettings(ServiceLocator injector) {
                EntityDictionary dictionary = new EntityDictionary(getCheckMappings());
                HashMapDataStore dataStore = new HashMapDataStore(Post.class.getPackage());
                dataStore.populateEntityDictionary(dictionary);

                ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                        .withUseFilterExpressions(true)
                        .withEntityDictionary(dictionary)
                        .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                        .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary));

                return builder.build();
            }

        });
        elide.start(false);
    }

    @AfterClass
    public void shutdown() throws Exception {
        elide.stop();
    }

    @Test
    public void testJsonAPIPost() {
        String result = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body("{\n" +
                  "         \"data\": {\n" +
                  "           \"type\": \"post\",\n" +
                  "           \"id\": \"1\",\n" +
                  "           \"attributes\": {\n" +
                  "             \"content\": \"This is my first post. woot.\",\n" +
                  "             \"date\" : \"0\"\n" +
                  "           }\n" +
                  "         }\n" +
                  "       }")
            .post("/api/v1/post")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString();
    }

    @Test
    public void testMetricsServlet() throws Exception {
        given()
                .when()
                .get("/stats/metrics")
                .then()
                .statusCode(200)
                .body("meters", hasKey("com.codahale.metrics.servlet.InstrumentedFilter.responseCodes.ok"));
    }

    @Test
    public void testHealthCheckServlet() throws Exception {
            given()
                .when()
                .get("/stats/healthcheck")
                .then()
                .statusCode(501); //Returns 'Not Implemented' if there are no Health Checks Registered
    }
}
