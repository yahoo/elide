/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.links;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.jsonapi.links.DefaultJSONApiLinks;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneAnalyticSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneAsyncSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import example.models.Post;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import io.restassured.response.Response;

import java.util.Properties;
import java.util.TimeZone;
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
            public ElideSettings getElideSettings(EntityDictionary dictionary, DataStore dataStore) {
                String jsonApiBaseUrl = getBaseUrl()
                        + getJsonApiPathSpec().replaceAll("/\\*", "")
                        + "/";

                ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(dictionary)
                        .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                        .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                        .withJSONApiLinks(new DefaultJSONApiLinks(jsonApiBaseUrl))
                        .withBaseUrl("https://elide.io")
                        .withAuditLogger(getAuditLogger())
                        .withJsonApiPath(getJsonApiPathSpec().replaceAll("/\\*", ""))
                        .withGraphqlApiPath(getGraphQLApiPathSpec().replaceAll("/\\*", ""));

                if (enableISO8601Dates()) {
                    builder = builder.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
                }

                return builder.build();
            }

            @Override
            public String getBaseUrl() {
                return "https://elide.io";
            }

            @Override
            public Properties getDatabaseProperties() {
                Properties options = new Properties();

                options.put("hibernate.show_sql", "true");
                options.put("hibernate.hbm2ddl.auto", "create");
                options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                options.put("hibernate.current_session_context_class", "thread");
                options.put("hibernate.jdbc.use_scrollable_resultset", "true");

                options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
                options.put("javax.persistence.jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;");
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
            public ElideStandaloneAsyncSettings getAsyncProperties() {
                ElideStandaloneAsyncSettings asyncPropeties = new ElideStandaloneAsyncSettings() {
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
                    public Integer getMaxRunTimeSeconds() {
                        return 1800;
                    }

                    @Override
                    public Integer getQueryCleanupDays() {
                        return 3;
                    }
                };
                return asyncPropeties;
            }

            @Override
            public ElideStandaloneAnalyticSettings getAnalyticProperties() {
                ElideStandaloneAnalyticSettings analyticPropeties = new ElideStandaloneAnalyticSettings() {
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
                return analyticPropeties;
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
            .body(equalTo(
                    data(
                            resource(
                                    type("postView"),
                                    id("0"),
                                    attributes(
                                            attr("content", "This is my first post. woot.")
                                    ),
                                    links(
                                            attr("self", "https://elide.io/api/v1/postView/0")
                                    )
                            )
                    ).toJSON()
                )
            );
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
                        "timeDimensionGrain", "timeDimension", "postView", "tableExport"));
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
                        .statusCode(com.yahoo.elide.core.exceptions.HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo(
                            data(
                                resource(
                                    type("post"),
                                    id("2"),
                                    attributes(
                                        attr("abusiveContent", false),
                                        attr("content", "This is my first post. woot."),
                                        attr("date", "2019-01-01T00:00Z")
                                    ),
                                    links(
                                        attr("self", "https://elide.io/api/v1/post/2")
                                    )
                                )
                            ).toJSON()
                        ));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus  contentLength } } } } }\","
                                + "\"variables\":null}")
                        .post("/graphql/api/v1/")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\",\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"result\":{\"responseBody\":\"{\\\"data\\\":[{\\\"type\\\":\\\"post\\\",\\\"id\\\":\\\"2\\\",\\\"attributes\\\":{\\\"abusiveContent\\\":false,\\\"content\\\":\\\"This is my first post. woot.\\\",\\\"date\\\":\\\"2019-01-01T00:00Z\\\"},\\\"links\\\":{\\\"self\\\":\\\"https://elide.io/api/v1/post/2\\\"}}]}\",\"httpStatus\":200,\"contentLength\":191}}}]}}}";
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
