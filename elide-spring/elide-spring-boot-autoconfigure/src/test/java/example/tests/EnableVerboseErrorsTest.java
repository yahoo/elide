/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.jsonapi.JsonApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests turning on verbose errors.
 */
@TestPropertySource(
        properties = {
                "elide.verbose-errors=true",
        }
)
public class EnableVerboseErrorsTest extends IntegrationTest {

    @Test
    public void verboseErrorsEnabledTest() {
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("group"),
                                        id("foo"),
                                        attributes(
                                                attr("deprecated", "Invalid!")
                                        )
                                )
                        )
                )
                .when()
                .post("/json/group")
                .then()
                .body("errors.detail[0]", equalTo("Invalid value: Invalid!\nCan&#39;t convert value &#39;Invalid!&#39; to type class java.lang.Boolean"))
                .log().all()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}
