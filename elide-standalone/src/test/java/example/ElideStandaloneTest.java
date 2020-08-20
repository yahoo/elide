/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import example.models.Post;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;

import java.util.Properties;

import javax.ws.rs.core.MediaType;

/**
 * Tests ElideStandalone starts and works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneTest {
    protected ElideStandalone elide;

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneSettings() {

            @Override
            public Properties getDatabaseProperties() {
                Properties options = new Properties();

                options.put("hibernate.show_sql", "true");
                options.put("hibernate.hbm2ddl.auto", "create");
                options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                options.put("hibernate.current_session_context_class", "thread");
                options.put("hibernate.jdbc.use_scrollable_resultset", "true");

                options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
                options.put("javax.persistence.jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE;");
                options.put("javax.persistence.jdbc.user", "sa");
                options.put("javax.persistence.jdbc.password", "");
                return options;
            }

            @Override
            public String getModelPackageName() {
                return Post.class.getPackage().getName();
            }

            @Override
            public boolean enableSwagger() {
                return true;
            }
            @Override
            public boolean enableGraphQL() {
                return true;
            }

            @Override
            public boolean enableJSONAPI() {
                return true;
            }

            @Override
            public boolean enableAsync() {
                return true;
            }

            @Override
            public boolean enableAsyncCleanup() {
                return true;
            }

            @Override
            public Integer getAsyncThreadSize() {
                return 3;
            }

            @Override
            public Integer getAsyncMaxRunTimeSeconds() {
                return 30;
            }

            @Override
            public Integer getAsyncQueryCleanupDays() {
                return 3;
            }

            @Override
            public AsyncQueryDAO getAsyncQueryDAO() {
                return null;
            }

            @Override
            public boolean enableDynamicModelConfig() {
                return true;
            }

            @Override
            public boolean enableAggregationDataStore() {
                return true;
            }

            @Override
            public String getDynamicConfigPath() {
                return "src/test/resources/models/";
            }
        });
        elide.start(false);
    }

    @AfterAll
    public void shutdown() throws Exception {
        elide.stop();
    }

    @Test
    public void testJsonAPIPost() {
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
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
            .post("/api/v1/post")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString();

        // Test the Dynamic Generated Analytical Model is accessible
        given()
            .when()
            .get("/api/v1/postView")
            .then()
            .statusCode(200)
            .body("data.id", hasItems("0"))
            .body("data.attributes.content", hasItems("This is my first post. woot."));

    }

    @Test
    public void testVersionedJsonAPIPost() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .header("ApiVersion", "1.0")
                .body(
                        datum(
                                resource(
                                        type("post"),
                                        id("2"),
                                        attributes(
                                                attr("text", "This is my first post. woot."),
                                                attr("date", "2019-01-01T00:00Z")
                                        )
                                )
                        )
                )
                .post("/api/v1/post")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();
    }

    @Test
    public void testForbiddenJsonAPIPost() {
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(
                datum(
                    resource(
                        type("post"),
                        id("2"),
                        attributes(
                            attr("content", "This is my first post. woot."),
                            attr("date", "2019-01-01T00:00Z"),
                            attr("abusiveContent", true)
                        )
                    )
                )
            )
            .post("/api/v1/post")
            .then()
            .statusCode(HttpStatus.SC_FORBIDDEN)
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

    @Test
    public void testSwaggerEndpoint() throws Exception {
        given()
                .when()
                .get("/swagger/doc/test")
                .then()
                .statusCode(200);
    }

    @Test
    public void swaggerDocumentTest() {
        when()
               .get("/swagger/doc/test")
                .then()
                .statusCode(200)
                .body("tags.name", containsInAnyOrder("post", "functionArgument", "metric",
                        "metricFunction", "dimension", "column", "table", "asyncQuery",
                        "timeDimensionGrain", "timeDimension", "postView"));
    }

    @Test
    public void testAsyncApiEndpoint() throws InterruptedException {
        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                        attributes(
                                                attr("query", "/post"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/api/v1/asyncQuery").asString();

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/api/v1/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Async Query is created and completed
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(com.yahoo.elide.core.HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                                + "[{\"type\":\"post\",\"id\":\"2\",\"attributes\":{\"abusiveContent\":false,"
                                + "\"content\":\"This is my first post. woot.\",\"date\":\"2019-01-01T00:00Z\"}}]}"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus resultType contentLength } } } } }\","
                                + "\"variables\":null}")
                        .post("/graphql/api/v1/")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\",\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"result\":{\"responseBody\":\"{\\\"data\\\":[{\\\"type\\\":\\\"post\\\",\\\"id\\\":\\\"2\\\",\\\"attributes\\\":{\\\"abusiveContent\\\":false,\\\"content\\\":\\\"This is my first post. woot.\\\",\\\"date\\\":\\\"2019-01-01T00:00Z\\\"}}]}\",\"httpStatus\":200,\"resultType\":\"EMBEDDED\",\"contentLength\":141}}}]}}}";
                assertEquals(expectedResponse, responseGraphQL);
                break;
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Async Query has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("Async Query not completed.");
            }
        }
    }
}
