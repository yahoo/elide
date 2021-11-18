/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.spring.controllers.JsonApiController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import io.restassured.RestAssured;

import java.nio.file.Path;
import java.util.TimeZone;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConfigStoreTest {

    private static Path testDirectory;

    @LocalServerPort
    protected int port;

    @BeforeAll
    public static void initialize(@TempDir Path testDirectory) {
        ConfigStoreTest.testDirectory = testDirectory;
        System.setProperty("elide.dynamic-config.path", testDirectory.toFile().getAbsolutePath());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Empty configuration load test.
     */
    @Test
    public void testEmptyConfiguration() {
        when()
                .get("http://localhost:" + port + "/json/config?fields[config]=path,type")
                .then()
                .body(equalTo("{\"data\":[]}"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testCreateFetchAndDelete() {
        String hjson = "{            \n"
                + "  tables: [{     \n"
                + "      name: Test\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INTEGER\n"
                + "          definition: 'MAX({{$measure}})'\n"
                + "         }\n"
                + "      ]      \n"
                + "      dimensions : [\n"
                + "         {\n"
                + "           name : dimension\n"
                + "           type : TEXT\n"
                + "           definition : '{{$dimension}}'\n"
                + "         }\n"
                + "      ]\n"
                + "  }]\n"
                + "}";

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        attributes(
                                                attr("path", "models/tables/table1.hjson"),
                                                attr("type", "TABLE"),
                                                attr("content", hjson)
                                        )
                                )
                        )
                )
                .when()
                .post("http://localhost:" + port + "/json/config")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        when()
                .get("http://localhost:" + port + "/json/config?fields[config]=content")
                .then()
                .body(equalTo(data(
                        resource(
                                type("config"),
                                id("bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24="),
                                attributes(
                                        attr("content", hjson)
                                )
                        )
                ).toJSON()))
                .statusCode(HttpStatus.SC_OK);

        when()
                .delete("http://localhost:" + port + "/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        when()
                .get("http://localhost:" + port + "/json/config?fields[config]=path,type")
                .then()
                .body(equalTo("{\"data\":[]}"))
                .statusCode(HttpStatus.SC_OK);

        when()
                .get("http://localhost:" + port + "/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
