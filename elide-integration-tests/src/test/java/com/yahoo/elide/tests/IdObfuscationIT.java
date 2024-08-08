/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.tests;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.patchOperation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.patchSet;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static com.yahoo.elide.test.jsonapi.elements.PatchOperationType.add;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.IdObfuscationTestApplicationResourceConfig;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.test.jsonapi.elements.Resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests for IdObfuscator.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IdObfuscationIT extends IntegrationTest {

    public IdObfuscationIT() {
        super(IdObfuscationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @BeforeEach
    void setup() {
        createBookEntities();
    }

    private void createBookEntities() {
        String tempAuthorId1 = "12345678-1234-1234-1234-1234567890ab";
        String tempAuthorId2 = "12345679-1234-1234-1234-1234567890ab";
        String tempAuthorId3 = "12345681-1234-1234-1234-1234567890ab";

        String tempBookId1 = "12345678-1234-1234-1234-1234567890ac";
        String tempBookId2 = "12345678-1234-1234-1234-1234567890ad";
        String tempBookId3 = "12345679-1234-1234-1234-1234567890ac";
        String tempBookId4 = "23451234-1234-1234-1234-1234567890ac";
        String tempBookId5 = "12345680-1234-1234-1234-1234567890ac";
        String tempBookId6 = "12345680-1234-1234-1234-1234567890ad";
        String tempBookId7 = "12345681-1234-1234-1234-1234567890ac";
        String tempBookId8 = "12345681-1234-1234-1234-1234567890ad";

        String tempPubId = "12345678-1234-1234-1234-1234567890ae";

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                patchSet(
                    patchOperation(add, "/author", resource(
                        type("author"),
                        id(tempAuthorId1),
                        attributes(
                            attr("name", "Ernest Hemingway")
                        ),
                        relationships(
                            relation("books",
                                linkage(type("book"), id(tempBookId1)),
                                linkage(type("book"), id(tempBookId2))
                            )
                        )
                    )),
                    patchOperation(add, "/book/", resource(
                        type("book"),
                        id(tempBookId1),
                        attributes(
                            attr("title", "The Old Man and the Sea"),
                            attr("genre", "Literary Fiction"),
                            attr("language", "English")
                        ),
                        relationships(
                            relation("publisher",
                                linkage(type("publisher"), id(tempPubId))

                            )
                        )
                    )),
                    patchOperation(add, "/book/", resource(
                        type("book"),
                        id(tempBookId2),
                        attributes(
                            attr("title", "For Whom the Bell Tolls"),
                            attr("genre", "Literary Fiction"),
                            attr("language", "English")
                        )
                    )),
                    patchOperation(add, "/book/" + tempBookId1 + "/publisher", resource(
                        type("publisher"),
                        id(tempPubId),
                        attributes(
                            attr("name", "Default publisher")
                        )
                    ))
                ).toJSON()
            )
            .patch("/")
            .then()
            .statusCode(OK_200);

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                patchSet(
                    patchOperation(add, "/author", resource(
                        type("author"),
                        id(tempAuthorId2),
                        attributes(
                            attr("name", "Orson Scott Card")
                        ),
                        relationships(
                            relation("books",
                                linkage(type("book"), id(tempBookId3)),
                                linkage(type("book"), id(tempBookId4))
                            )
                        )
                    )),
                    patchOperation(add, "/book", resource(
                        type("book"),
                        id(tempBookId3),
                        attributes(
                            attr("title", "Enders Game"),
                            attr("genre", "Science Fiction"),
                            attr("language", "English"),
                            attr("publishDate", 1454638927412L)
                        )
                    )),
                    patchOperation(add, "/book", resource(
                        type("book"),
                        id(tempBookId4),
                        attributes(
                            attr("title", "Enders Shadow"),
                            attr("genre", "Science Fiction"),
                            attr("language", "English"),
                            attr("publishDate", 1464638927412L)
                        )
                    ))
                )
            )
            .patch("/")
            .then()
            .statusCode(OK_200);

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                patchSet(
                    patchOperation(add, "/author", resource(
                        type("author"),
                        id(tempAuthorId3),
                        attributes(
                            attr("name", "Isaac Asimov")
                        ),
                        relationships(
                            relation("books",
                                linkage(type("book"), id(tempBookId5)),
                                linkage(type("book"), id(tempBookId6))
                            )
                        )
                    )),
                    patchOperation(add, "/book", resource(
                        type("book"),
                        id(tempBookId5),
                        attributes(
                            attr("title", "Foundation"),
                            attr("genre", "Science Fiction"),
                            attr("language", "English")
                        )
                    )),
                    patchOperation(add, "/book", resource(
                        type("book"),
                        id(tempBookId6),
                        attributes(
                            attr("title", "The Roman Republic"),
                            //genre null
                            attr("language", "English")
                        )
                    ))
                )
            )
            .patch("/")
            .then()
            .statusCode(OK_200);

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                patchSet(
                    patchOperation(add, "/author", resource(
                        type("author"),
                        id(tempAuthorId3),
                        attributes(
                            attr("name", "Null Ned")
                        ),
                        relationships(
                            relation("books",
                                linkage(type("book"), id(tempBookId7)),
                                linkage(type("book"), id(tempBookId8))
                            )
                        )
                    )),
                    patchOperation(add, "/book", resource(
                        type("book"),
                        id(tempBookId7),
                        attributes(
                            attr("title", "Life with Null Ned"),
                            attr("language", "English")
                        )
                    )),
                    patchOperation(add, "/book", resource(
                        type("book"),
                        id(tempBookId8),
                        attributes(
                            attr("title", "Life with Null Ned 2"),
                            attr("genre", "Not Null"),
                            attr("language", "English")
                        )
                    ))
                ).toJSON()
            )
            .patch("/")
            .then()
            .statusCode(OK_200);
    }

    @Test
    void testIdObfuscation() {
        // Create
        String dataId = given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("customerInvoice"),
                                        attributes(
                                                attr("complete", true),
                                                attr("total", 1000)
                                        )
                                )
                        )
                )
                .post("/customerInvoice")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.attributes.total", equalTo(1100))
                .body("data.attributes.complete", equalTo(true))
                .extract()
                .path("data.id");
        assertTrue(dataId.length() > 10);
        // Get
        Resource resource = resource(
                type("customerInvoice"),
                id(dataId),
                attributes(
                        attr("complete", true),
                        attr("total", 1100)
                )
        );
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .get("/customerInvoice/" + dataId)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(resource).toJSON()));

        // Patch
        Resource modified = resource(
                type("customerInvoice"),
                id(dataId),
                attributes(
                        attr("complete", true),
                        attr("total", 123456)
                )
        );

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(datum(modified))
                .patch("/customerInvoice/" + dataId)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Get again
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .get("/customerInvoice/" + dataId)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(modified).toJSON()));

        // Get list
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .get("/customerInvoice")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(data(modified).toJSON()));

        // Delete
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .delete("/customerInvoice/" + dataId)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    void testPaginationCursorFirst() {
        String url = "/book?page[first]=2";
        when()
            .get(url)
            .then()
            .body("data", hasSize(2),
                  "meta.page.startCursor", not(emptyString()),
                  "meta.page.endCursor", not(emptyString())
            );
    }

    @Test
    void testPaginationCursorLast() {
        String url = "/book?page[last]=2";
        when()
            .get(url)
            .then()
            .body("data", hasSize(2),
                  "meta.page.startCursor", not(emptyString()),
                  "meta.page.endCursor", not(emptyString())
            );
    }

    @Test
    void testPaginationCursorAfter() {
        String url = "/book?page[first]=2";
        String endCursor = get(url).path("meta.page.endCursor");
        String url2 = "/book?page[size]=2&page[after]=" + endCursor;
        when()
            .get(url2)
            .then()
            .body("data", hasSize(2),
                  "meta.page.startCursor", not(emptyString()),
                  "meta.page.endCursor", not(emptyString())
            );
    }

    @Test
    void testPaginationCursorBefore() {
        String url = "/book?page[last]=2";
        String startCursor = get(url).path("meta.page.startCursor");
        String url2 = "/book?page[size]=2&page[before]=" + startCursor;
        when()
            .get(url2)
            .then()
            .body("data", hasSize(2),
                  "meta.page.startCursor", not(emptyString()),
                  "meta.page.endCursor", not(emptyString())
            );
    }

    @Test
    void testPaginationCursorAfterBefore() {
        String url = "/book?page[last]=3";
        String startCursor = get(url).path("meta.page.startCursor");
        String endCursor = get(url).path("meta.page.endCursor");
        String url2 = "/book?page[size]=2&page[after]=" + startCursor + "&page[before]=" + endCursor;
        when()
            .get(url2)
            .then()
            .body("data", hasSize(1),
                  "meta.page.startCursor", not(emptyString()),
                  "meta.page.endCursor", not(emptyString())
            );
    }
}
