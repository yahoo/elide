/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.document;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.include;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.links;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.patchOperation;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.patchSet;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.elements.PatchOperationType.add;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Relation.TO_ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JsonApiDSLTest {

    @Test
    public void verifyBasicRequest() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":1}}";

        String actual = datum(
                resource(
                        type("blog"),
                        id(1)
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithAttributes() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"attributes\":"
                + "{\"title\":\"Why You Should use Elide\",\"date\":\"2019-01-01\"}}}";

        String actual = datum(
                resource(
                        type("blog"),
                        id("1"),
                        attributes(
                                attr("title", "Why You Should use Elide"),
                                attr("date", "2019-01-01")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithOneToOneRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{\"author\":{\"data\":{\"type\":\"author\",\"id\":\"1\"}}}}}";

        String actual = datum(
                resource(
                        type("blog"),
                        id("1"),
                        attributes(
                                attr("title", "title")
                        ),
                        relationships(
                                relation("author",
                                        TO_ONE,
                                        linkage(type("author"), id("1"))
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithOneToManyRelationship() {
        // multi-elements array of resource identifier objects for non-empty to-many relationships.
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{"
                + "\"comments\":{\"data\":[{\"type\":\"comment\",\"id\":\"1\"},"
                + "{\"type\":\"comment\",\"id\":\"2\"}]}}}}";

        String actual = datum(
                resource(
                        type("blog"),
                        id("1"),
                        attributes(
                                attr("title", "title")
                        ),
                        relationships(
                              relation("comments",
                                      linkage(type("comment"), id("1")),
                                      linkage(type("comment"), id("2"))
                              )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);

        // single-element array of resource identifier objects for non-empty to-many relationships.
        expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{"
                + "\"comments\":{\"data\":[{\"type\":\"comment\",\"id\":\"1\"}]}}}}";

        actual = datum(
                resource(
                        type("blog"),
                        id("1"),
                        attributes(
                                attr("title", "title")
                        ),
                        relationships(
                                relation("comments",
                                        linkage(type("comment"), id("1"))
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithManyRelationships() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{"
                + "\"author\":{\"data\":[{\"type\":\"author\",\"id\":\"1\"}]},"
                + "\"comments\":{\"data\":[{\"type\":\"comment\",\"id\":\"2\"}]}}}}";

        String actual = datum(
                resource(
                        type("blog"),
                        id("1"),
                        attributes(
                                attr("title", "title")
                        ),
                        relationships(
                                relation("author",
                                        linkage(type("author"), id("1"))
                                ),
                                relation("comments",
                                        linkage(type("comment"), id("2"))
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyResourcesWithIncludes() {
        String expected = "{\"data\":[{\"type\":\"blog\",\"id\":1}],\"included\":[{\"type\":\"author\",\"id\":1}]}";

        String actual = document(
                data(
                    resource(
                            type("blog"),
                            id(1)
                    )
                ),
                include(
                        resource(
                                type("author"),
                                id(1)
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyDataWithRelationshipLinks() {
        String expected = "{\"data\":[{\"type\":\"blog\",\"id\":1},{\"type\":\"blog\",\"id\":2}]}";

        String actual =
                data(
                        linkage(
                                type("blog"),
                                id(1)
                        ),
                        linkage(
                                type("blog"),
                                id(2)
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyEmptyToOneRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},\"relationships\":{\"author\":{\"data\":null}}}}";

        String actual =
                datum(
                        resource(
                                type("blog"),
                                id("1"),
                                attributes(
                                        attr("title", "title")
                                ),
                                relationships(
                                        relation("author", TO_ONE)
                                )
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyEmptyToManyRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},\"relationships\":{\"comments\":{\"data\":[]}}}}";

        String actual =
                datum(
                        resource(
                                type("blog"),
                                id("1"),
                                attributes(
                                        attr("title", "title")
                                ),
                                relationships(
                                        relation("comments")
                                )
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyRelationshipsButNoAttributes() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"relationships\":{\"comments\":{\"data\":[]},"
                + "\"author\":{\"data\":[{\"type\":\"author\",\"id\":\"1\"}]}}}}";

        String actual =
                datum(
                        resource(
                                type("blog"),
                                id("1"),
                                relationships(
                                        relation("comments"),
                                        relation("author",
                                                linkage(type("author"), id("1"))
                                        )
                                )
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyNoId() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":null,\"attributes\""
                + ":{\"title\":\"Why You Should use Elide\",\"date\":\"2019-01-01\"}}}";

        String actual = datum(
                resource(
                        type("blog"),
                        attributes(
                                attr("title", "Why You Should use Elide"),
                                attr("date", "2019-01-01")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyPatchOperation() {
        String expected = "[{\"op\":\"add\",\"path\":\"/parent\",\"value\":{\"type\":\"parent\",\"id\":\"1\",\"relationships\":{\"children\":{\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},\"spouses\":{\"data\":[{\"type\":\"parent\",\"id\":\"3\"}]}}}},{\"op\":\"add\",\"path\":\"/parent/1/children\",\"value\":{\"type\":\"child\",\"id\":\"2\"}}]";

        String actual = patchSet(
                patchOperation(add, "/parent",
                        resource(
                                type("parent"),
                                id("1"),
                                relationships(
                                        relation("children",
                                                linkage(type("child"), id("2"))
                                        ),
                                        relation("spouses",
                                                linkage(type("parent"), id("3"))
                                        )
                                )
                        )
                ),
                patchOperation(add, "/parent/1/children",
                        resource(
                                type("child"),
                                id("2")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithLinks() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"attributes\":"
                + "{\"title\":\"Why You Should use Elide\",\"date\":\"2019-01-01\"},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/api/v1/blog/1\"}}}";

        String actual = datum(
                resource(
                        type("blog"),
                        id("1"),
                        attributes(
                                attr("title", "Why You Should use Elide"),
                                attr("date", "2019-01-01")
                        ),
                        links(
                                attr("self", "http://localhost:8080/json/api/v1/blog/1")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithLinksRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"attributes\":"
                + "{\"title\":\"Why You Should use Elide\"},"
                + "\"relationships\":{"
                +   "\"author\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/api/v1/blog/1/relationships/author\",\"related\":\"http://localhost:8080/json/api/v1/blog/1/author\"},"
                +       "\"data\":[{\"type\":\"author\",\"id\":\"1\"}]"
                +   "},"
                +   "\"comments\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/api/v1/blog/1/relationships/comments\",\"related\":\"http://localhost:8080/json/api/v1/blog/1/comments\"},"
                +       "\"data\":[]"
                +   "}},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/api/v1/blog/1\"}}}";

        String actual = datum(
                resource(
                        type("blog"),
                        id("1"),
                        attributes(
                                attr("title", "Why You Should use Elide")
                        ),
                        links(
                                attr("self", "http://localhost:8080/json/api/v1/blog/1")
                        ),
                        relationships(
                                relation("author",
                                        links(
                                                attr("self", "http://localhost:8080/json/api/v1/blog/1/relationships/author"),
                                                attr("related", "http://localhost:8080/json/api/v1/blog/1/author")
                                        ),
                                        linkage(type("author"), id("1"))
                                ),
                                relation("comments",
                                        links(
                                                attr("self", "http://localhost:8080/json/api/v1/blog/1/relationships/comments"),
                                                attr("related", "http://localhost:8080/json/api/v1/blog/1/comments")
                                        )
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }
}
