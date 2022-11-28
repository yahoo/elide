/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static io.restassured.RestAssured.when;

import com.yahoo.elide.core.exceptions.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Executes Export Controller tests with Async Enabled but Export Controller Disabled.
 */
@TestPropertySource(
        properties = {
                "elide.async.enabled=true",
                "elide.async.export.enabled=false"
        }
)
public class DisableExportControllerTest extends IntegrationTest {

    @Test
    public void exportControllerTest() {
        // A post to export will result in not found, if controller was disabled.
        // If controller is enabled, it returns Method Not Allowed.
        when()
                .post("/export/1")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
