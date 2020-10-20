/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.yahoo.elide.core.HttpStatus;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * Executes Controller tests with Aggregation Store disabled.
 */
@ActiveProfiles("disableAggStore")
public class DisableAggStoreControllerTest extends ControllerTest {

    @Override
    @Test
    public void swaggerDocumentTest() {
        when()
                .get("/doc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("tags.name", containsInAnyOrder("group", "asyncQuery", "product", "version"));
    }
}
