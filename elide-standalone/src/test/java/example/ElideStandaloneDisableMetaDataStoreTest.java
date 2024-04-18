/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.paiondata.elide.standalone.ElideStandalone;
import com.paiondata.elide.standalone.config.ElideStandaloneAnalyticSettings;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.http.ContentType;

/**
 * Tests ElideStandalone starts and works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneDisableMetaDataStoreTest extends ElideStandaloneTest {

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneTestSettings() {

            @Override
            public ElideStandaloneAnalyticSettings getAnalyticProperties() {
                ElideStandaloneAnalyticSettings analyticProperties = new ElideStandaloneAnalyticSettings() {
                    @Override
                    public boolean enableDynamicModelConfig() {
                        return true;
                    }

                    @Override
                    public boolean enableAggregationDataStore() {
                        return true;
                    }

                    @Override
                    public boolean enableMetaDataStore() {
                        return false;
                    }

                    @Override
                    public String getDynamicConfigPath() {
                        return "src/test/resources/configs/";
                    }
                };
                return analyticProperties;
            }
        });
        elide.start(false);
    }

    @Override
    @Test
    public void apiDocsDocumentTest() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api-docs")
                .then()
                .statusCode(200)
                .body("tags.name", containsInAnyOrder("post", "asyncQuery", "postView"));
    }

    @Override
    @Test
    public void metaDataTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/api/namespace/default") //"default" namespace added by Agg Store.
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND); // Metadatastore is disabled, so not found.
    }
}
