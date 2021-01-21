/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * Executes Async tests with Aggregation Store disabled.
 */
@ActiveProfiles("disableGraphQL")
public class DisableGraphQLAsyncTest extends IntegrationTest {

    @Test
    public void testAsyncGraphQL() throws InterruptedException {
        String expected = "{\"errors\":[{\"detail\":\"Invalid operation: GraphQL is disabled. Please enable GraphQL in settings.\"}]}";
        //Create Async Request
        given()
        .contentType(JSONAPI_CONTENT_TYPE)
        .body(
                data(
                        resource(
                                type("asyncQuery"),
                                id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                attributes(
                                        attr("query", "/group"),
                                        attr("queryType", "GRAPHQL_V1_0"),
                                        attr("status", "QUEUED"),
                                        attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
        .when()
        .post("/json/asyncQuery")
        .then()
        .statusCode(org.apache.http.HttpStatus.SC_BAD_REQUEST)
        .body(equalTo(expected));
    }
}
