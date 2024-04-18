/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for UserType.
 */
class UserTypeIT extends IntegrationTest {

    @Data
    @AllArgsConstructor
    private class Address {
        private String street;
        private String state;
        private Zip zip;

    }

    @Data
    @AllArgsConstructor
    private class Zip {
        private String zip;
        private String plusFour;
    }

    @Test
    @Tag("skipInMemory")
    public void testUserTypePost() throws Exception {
        Resource resource = resource(
                type("person"),
                id("1"),
                attributes(
                        attr("name", "AK"),
                        attr("address", new Address(
                                "123 AnyStreet Dr",
                                "IL",
                                new Zip("61820", "1234")
                        )),
                        attr("alternateAddress", new Address(
                                "XYZ AnyStreet Dr",
                                "IL",
                                new Zip("61820", "1234")
                        ))
                )
        );

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(datum(resource))
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/person/1")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(jsonEquals(datum(resource), true));
    }

    @Test
    @Tag("skipInMemory")
    public void testUserTypePatch() throws Exception {
        Resource original = resource(
                type("person"),
                id("2"),
                attributes(
                        attr("name", "JK"),
                        attr("address", new Address(
                                "456 AnyStreet Dr",
                                "IL",
                                new Zip("61822", "567")
                        )),
                        attr("alternateAddress", new Address(
                                "XYZ AnyStreet Dr",
                                "IL",
                                new Zip("61820", "1234")
                        ))
                )
        );

        Resource modified = resource(
                type("person"),
                id("2"),
                attributes(
                        attr("name", "DC"),
                        attr("address", new Address(
                                "456 AnyRoad Ave",
                                "AZ",
                                new Zip("85001", "9999")
                        )),
                        attr("alternateAddress", new Address(
                                "ABC AnyStreet Dr",
                                "CO",
                                new Zip("12345", "1234")
                        ))
                )
        );

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(datum(original))
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(datum(modified))
            .patch("/person/2")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/person/2")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(jsonEquals(datum(modified), true));
    }

    @Test
    @Tag("skipInMemory")
    public void testUserTypeMissingUserTypeField() throws Exception {
        Resource resource = resource(
                type("person"),
                id("3"),
                attributes(
                        attr("name", "DM")
                )
        );

        Resource expected = resource(
                type("person"),
                id("3"),
                attributes(
                        attr("name", "DM"),
                        attr("address", null),
                        attr("alternateAddress", null)
                )
        );


        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(datum(resource))
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/person/3")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(jsonEquals(datum(expected), true));
    }

    @Test
    @Tag("skipInMemory")
    public void testUserTypeMissingUserTypeProperties() throws Exception {

        Map<String, Object> partialZip = new HashMap<>();
        partialZip.put("zip", "60412");

        Map<String, Object> partialAddress = new HashMap<>();
        partialAddress.put("street", "1400 AnyAve St");
        partialAddress.put("zip", partialZip);

        Resource resource = resource(
            type("person"),
            id("4"),
            attributes(
                attr("name", "WC"),
                attr("address", partialAddress),
                attr("alternateAddress", partialAddress)
            )
        );

        Resource expected = resource(
            type("person"),
            id("4"),
            attributes(
                attr("name", "WC"),
                attr("address", new Address(
                    "1400 AnyAve St",
                    null,
                    new Zip("60412", null)
                )),
                attr("alternateAddress", new Address(
                    "1400 AnyAve St",
                    null,
                    new Zip("60412", null)
                ))
            )
        );

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(datum(resource))
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/person/4")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(jsonEquals(datum(expected), true));
    }
}
