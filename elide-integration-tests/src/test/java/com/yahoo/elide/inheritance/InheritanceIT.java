package com.yahoo.elide.inheritance;

import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;

@Slf4j
public class InheritanceIT extends AbstractIntegrationTestInitializer {
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    @Test
    public void testResponseCodeOnUpdate() {
        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("employee"),
                                        id(null)
                                )
                        )
                )
                .post("/assignedIdLong")
                .then().extract().asString();

        log.info(actual);
    }
}
