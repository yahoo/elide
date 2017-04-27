/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests
import com.yahoo.elide.core.HttpStatus
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
/**
 * Tests for UserType
 */
class UserTypeIT extends AbstractIntegrationTestInitializer {

    @Test
    public void testUserTypePost() {

        String person = """
        {
            "data": {
                "id": 1,
                "type": "person",
                "attributes": {
                    "name": "AK",
                    "address": {
                        "street": "123 AnyStreet Dr",
                        "state": "IL",
                        "zip": {
                            "zip": "61820",
                            "plusFour": "1234"
                        }
                    }
                }
            }
        }
        """;

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(person)
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        String resp = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/person/1")
            .then()
            .statusCode(HttpStatus.SC_OK).extract().body().asString();
        assertEqualDocuments(resp, person);
    }

    @Test
    public void testUserTypePatch() {

        String originalPerson = """
        {
            "data": {
                "id": 2,
                "type": "person",
                "attributes": {
                    "name": "JK",
                    "address": {
                        "street": "456 AnyStreet Dr",
                        "state": "IL",
                        "zip": {
                            "zip": "61822",
                            "plusFour": "567"
                        }
                    }
                }
            }
        }
        """;

        String updatedPerson = """
        {
            "data": {
                "id": 2,
                "type": "person",
                "attributes": {
                    "name": "DC",
                    "address": {
                        "street": "456 AnyRoad Ave",
                        "state": "AZ",
                        "zip": {
                            "zip": "85001",
                            "plusFour": "9999"
                        }
                    }
                }
            }
        }
        """;

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(originalPerson)
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(updatedPerson)
            .patch("/person/2")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        String resp = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/person/2")
            .then()
            .statusCode(HttpStatus.SC_OK).extract().body().asString();
        assertEqualDocuments(resp, updatedPerson);
     }

    @Test
    public void testUserTypeMissingUserTypeField() {

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body( """
            {
                "data": {
                    "id": 3,
                    "type": "person",
                    "attributes": {
                        "name": "DM"
                    }
                }
            }
            """)
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        String resp = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/person/3")
            .then()
            .statusCode(HttpStatus.SC_OK).extract().body().asString();
        assertEqualDocuments(resp, """
            {
                "data": {
                    "type": "person",
                    "id": "3",
                    "attributes": {
                        "address": null,
                        "name": "DM"
                    }
                }
            }
            """);
    }

    @Test
    public void testUserTypeMissingUserTypeProperties() {

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body( """
            {
                "data": {
                    "id": 4,
                    "type": "person",
                    "attributes": {
                        "name": "WC",
                        "address": {
                            "street": "1400 AnyAve St",
                            "zip": {
                                "zip": "60412"
                            }
                        }
                    }
                }
            }
            """)
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        String resp = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/person/4")
            .then()
            .statusCode(HttpStatus.SC_OK).extract().body().asString();

        assertEqualDocuments(resp, """
            {
                "data": {
                    "id": 4,
                    "type": "person",
                    "attributes": {
                        "name": "WC",
                        "address": {
                            "street": "1400 AnyAve St",
                            "state": null,
                            "zip": {
                                "zip": "60412",
                                "plusFour": null
                            }
                        }
                    }
                }
            }
        """);
    }
}
