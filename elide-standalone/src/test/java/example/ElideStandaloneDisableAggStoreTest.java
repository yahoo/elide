/*
 * Copyright 2020, Oath Inc.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.AsyncProperties;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import example.models.Post;

import org.apache.http.HttpStatus;
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
public class ElideStandaloneDisableAggStoreTest extends ElideStandaloneTest {

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
            public AsyncProperties getAsyncProperties() {
                //Default Properties
                AsyncProperties asyncPropeties = new AsyncProperties();
                asyncPropeties.setEnabled(true);
                asyncPropeties.setCleanupEnabled(true);
                asyncPropeties.setThreadPoolSize(3);
                asyncPropeties.setMaxRunTimeSeconds(1800);
                asyncPropeties.setQueryCleanupDays(3);
                asyncPropeties.getDownload().setEnabled(false);
                return asyncPropeties;
            }

            @Override
            public boolean enableDynamicModelConfig() {
                return true;
            }

            @Override
            public boolean enableAggregationDataStore() {
                return false;
            }

            @Override
            public String getDynamicConfigPath() {
                return "src/test/resources/models/";
            }
        });
        elide.start(false);
    }

    @Override
    @Test
    public void swaggerDocumentTest() {
        when()
        .get("/swagger/doc/test")
         .then()
         .statusCode(200)
         .body("tags.name", containsInAnyOrder("post", "asyncQuery"));
    }

    @Override
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
    }

    @Override
    @Test
    public void testDownloadEndpoint() throws Exception {
        given()
                .when()
                .get("/download/test")
                .then()
                .statusCode(404);
    }

    @Override
    @Test
    public void testAsyncDownloadSuccessful() throws InterruptedException {
        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9264d"),
                                        attributes(
                                                attr("query", "/post"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("resultType", "DOWNLOAD")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/api/v1/asyncQuery")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/api/v1/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9264d");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Async Query is created and completed
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(com.yahoo.elide.core.HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9264d"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody",
                                equalTo("http://localhost:8080/download/ba31ca4e-ed8f-4be0-a0f3-12088fa9264d"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9264d\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ responseBody httpStatus contentLength } } } } }\","
                                + "\"variables\":null}")
                        .post("/graphql/api/v1/")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9264d\",\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"DOWNLOAD\",\"result\":{\"responseBody\":\"http://localhost:8080/download/ba31ca4e-ed8f-4be0-a0f3-12088fa9264d\",\"httpStatus\":200,\"contentLength\":272}}}]}}}";
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

        given()
                .when()
                .get("/download/ba31ca4e-ed8f-4be0-a0f3-12088fa9264d")
                .then()
                .statusCode(404);
    }
}
