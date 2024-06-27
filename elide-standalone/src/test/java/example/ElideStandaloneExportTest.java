/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneAnalyticSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneAsyncSettings;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.ws.rs.core.MediaType;

import java.time.Duration;

/**
 * Tests ElideStandalone starts and works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneExportTest {
    protected ElideStandalone elide;

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneTestSettings() {

            @Override
            public ElideStandaloneAsyncSettings getAsyncProperties() {
                ElideStandaloneAsyncSettings asyncProperties = new ElideStandaloneAsyncSettings() {
                    @Override
                    public boolean enabled() {
                        return true;
                    }

                    @Override
                    public boolean enableCleanup() {
                        return true;
                    }

                    @Override
                    public Integer getThreadSize() {
                        return 5;
                    }

                    @Override
                    public Duration getQueryMaxRunTime() {
                        return Duration.ofSeconds(1800L);
                    }

                    @Override
                    public Duration getQueryRetentionDuration() {
                        return Duration.ofDays(3L);
                    }

                    @Override
                    public boolean enableExport() {
                        return true;
                    }
                };
                return asyncProperties;
            }

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
                        return true;
                    }

                    @Override
                    public String getDefaultDialect() {
                        return SQLDialectFactory.getDefaultDialect().getDialectType();
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

    @AfterAll
    public void shutdown() throws Exception {
        elide.stop();
    }

    @Test
    public void exportNotFound() {
        int queryId = 1;
        when()
                .get("/export/" + queryId)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                // Special Message generated by Export API
                .body(containsString(queryId + " Not Found"));
    }

    @Test
    public void testExportDynamicModel() throws InterruptedException {
        // Load Test data in Post Table
        given()
        .contentType(JsonApi.MEDIA_TYPE)
        .accept(JsonApi.MEDIA_TYPE)
        .body(
            datum(
                resource(
                    type("post"),
                    id("1"),
                    attributes(
                        attr("content", "This is my first post. woot."),
                        attr("date", "2019-01-01T00:00Z")
                    )
                )
            )
        )
        .post("/api/post")
        .then()

        .statusCode(HttpStatus.SC_CREATED);

        //Create Table Export
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9265d"),
                                        attributes(
                                                attr("query", "{\"query\":\"{postView{ edges{node{content}}}}\",\"variables\":null}"),
                                                attr("queryType", "GRAPHQL_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/api/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/api/tableExport/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d");
            String outputResponse = response.jsonPath().getString("data.attributes.status");
             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {
                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9265d"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("GRAPHQL_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9265d\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql/api")
                        .asString();
                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9265d\","
                        + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"CSV\","
                        + "\"result\":{\"url\":\"https://elide.io/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d\",\"httpStatus\":200,\"recordCount\":1}}}]}}}";
                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
        when()
                .get("/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    // TableExport should be available in OpenAPI Doc
    @Test
    public void apiDocsDocumentTest() {
        given()
               .accept(ContentType.JSON)
               .when()
               .get("/api-docs")
                .then()
                .statusCode(200)
                .body("tags.name",
                        containsInAnyOrder("atomic", "post", "argument", "metric", "dimension", "column", "table",
                                "asyncQuery", "timeDimensionGrain", "timeDimension", "postView", "tableExport",
                                "namespace", "tableSource"));
    }
}
