/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.fieldLevelTest;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.utils.JsonParser;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.test.jsonapi.elements.Resource;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class FieldLevelParentClassIdIT extends IntegrationTest {
    private final JsonParser jsonParser = new JsonParser();

    @Test
    public void testResponseCodeOnUpdate() {
        Resource original = resource(
                type("fieldLevelChild"),
                id("1"),
                attributes(
                        attr("childField", "someValue"),
                        attr("parentField", "parentValue")
                )
        );

        Resource modified = resource(
                type("fieldLevelChild"),
                id("1"),
                attributes(
                        attr("childField", "someOtherValue"),
                        attr("parentField", "aNewParentValue")
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(original))
                .post("/fieldLevelChild")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(original).toJSON()));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(modified))
                .patch("/fieldLevelChild/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
