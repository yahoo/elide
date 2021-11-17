/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import com.yahoo.elide.core.exceptions.HttpStatus;
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
}
