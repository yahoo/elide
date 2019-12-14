/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;

import com.yahoo.elide.contrib.swagger.SwaggerBuilder;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import com.yahoo.elide.standalone.models.Post;

import com.google.common.collect.Maps;
import io.swagger.models.Info;
import io.swagger.models.Swagger;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tests ElideStandalone starts and works
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneTest {
    private ElideStandalone elide;

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

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
            public Map<String, Swagger> enableSwagger() {
                EntityDictionary dictionary = new EntityDictionary(Maps.newHashMap());

                dictionary.bindEntity(Post.class);
                Info info = new Info().title("Test Service").version("1.0");

                SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
                Swagger swagger = builder.build();

                Map<String, Swagger> docs = new HashMap<>();
                docs.put("test", swagger);
                return docs;
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
}

