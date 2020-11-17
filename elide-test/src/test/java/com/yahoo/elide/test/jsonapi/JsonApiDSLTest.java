/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.test.jsonapi;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.test.jsonapi.elements.PatchOperationType.add;
import static com.yahoo.elide.test.jsonapi.elements.Relation.TO_ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JsonApiDSLTest {

    @Test
    public void verifyBasicRequest() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":1}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id(1)
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithAttributes() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"attributes\":"
                + "{\"title\":\"Why You Should use Elide\",\"date\":\"2019-01-01\"}}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "Why You Should use Elide"),
                                JsonApiDSL.attr("date", "2019-01-01")
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

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "title")
                        ),
                        JsonApiDSL.relationships(
                                relation("author",
                                        TO_ONE,
                                        JsonApiDSL.linkage(JsonApiDSL.type("author"), JsonApiDSL.id("1"))
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

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "title")
                        ),
                        JsonApiDSL.relationships(
                              JsonApiDSL.relation("comments",
                                      JsonApiDSL.linkage(JsonApiDSL.type("comment"), JsonApiDSL.id("1")),
                                      JsonApiDSL.linkage(JsonApiDSL.type("comment"), JsonApiDSL.id("2"))
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

        actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "title")
                        ),
                        JsonApiDSL.relationships(
                                JsonApiDSL.relation("comments",
                                        JsonApiDSL.linkage(JsonApiDSL.type("comment"), JsonApiDSL.id("1"))
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

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "title")
                        ),
                        JsonApiDSL.relationships(
                                JsonApiDSL.relation("author",
                                        JsonApiDSL.linkage(JsonApiDSL.type("author"), JsonApiDSL.id("1"))
                                ),
                                JsonApiDSL.relation("comments",
                                        JsonApiDSL.linkage(JsonApiDSL.type("comment"), JsonApiDSL.id("2"))
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyResourcesWithIncludes() {
        String expected = "{\"data\":[{\"type\":\"blog\",\"id\":1}],\"included\":[{\"type\":\"author\",\"id\":1}]}";

        String actual = JsonApiDSL.document(
                JsonApiDSL.data(
                    JsonApiDSL.resource(
                            JsonApiDSL.type("blog"),
                            JsonApiDSL.id(1)
                    )
                ),
                JsonApiDSL.include(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("author"),
                                JsonApiDSL.id(1)
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyDataWithRelationshipLinks() {
        String expected = "{\"data\":[{\"type\":\"blog\",\"id\":1},{\"type\":\"blog\",\"id\":2}]}";

        String actual =
                JsonApiDSL.data(
                        JsonApiDSL.linkage(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id(1)
                        ),
                        JsonApiDSL.linkage(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id(2)
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyEmptyToOneRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},\"relationships\":{\"author\":{\"data\":null}}}}";

        String actual =
                JsonApiDSL.datum(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id("1"),
                                JsonApiDSL.attributes(
                                        JsonApiDSL.attr("title", "title")
                                ),
                                JsonApiDSL.relationships(
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
                JsonApiDSL.datum(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id("1"),
                                JsonApiDSL.attributes(
                                        JsonApiDSL.attr("title", "title")
                                ),
                                JsonApiDSL.relationships(
                                        JsonApiDSL.relation("comments")
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
                JsonApiDSL.datum(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id("1"),
                                JsonApiDSL.relationships(
                                        JsonApiDSL.relation("comments"),
                                        JsonApiDSL.relation("author",
                                                JsonApiDSL.linkage(JsonApiDSL.type("author"), JsonApiDSL.id("1"))
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

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "Why You Should use Elide"),
                                JsonApiDSL.attr("date", "2019-01-01")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyPatchOperation() {
        String expected = "[{\"op\":\"add\",\"path\":\"/parent\",\"value\":{\"type\":\"parent\",\"id\":\"1\",\"relationships\":{\"children\":{\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},\"spouses\":{\"data\":[{\"type\":\"parent\",\"id\":\"3\"}]}}}},{\"op\":\"add\",\"path\":\"/parent/1/children\",\"value\":{\"type\":\"child\",\"id\":\"2\"}}]";

        String actual = JsonApiDSL.patchSet(
                JsonApiDSL.patchOperation(add, "/parent",
                        JsonApiDSL.resource(
                                JsonApiDSL.type("parent"),
                                JsonApiDSL.id("1"),
                                JsonApiDSL.relationships(
                                        JsonApiDSL.relation("children",
                                                JsonApiDSL.linkage(JsonApiDSL.type("child"), JsonApiDSL.id("2"))
                                        ),
                                        JsonApiDSL.relation("spouses",
                                                JsonApiDSL.linkage(JsonApiDSL.type("parent"), JsonApiDSL.id("3"))
                                        )
                                )
                        )
                ),
                JsonApiDSL.patchOperation(add, "/parent/1/children",
                        JsonApiDSL.resource(
                                JsonApiDSL.type("child"),
                                JsonApiDSL.id("2")
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

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "Why You Should use Elide"),
                                JsonApiDSL.attr("date", "2019-01-01")
                        ),
                        JsonApiDSL.links(
                                JsonApiDSL.attr("self", "http://localhost:8080/json/api/v1/blog/1")
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

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "Why You Should use Elide")
                        ),
                        JsonApiDSL.links(
                                JsonApiDSL.attr("self", "http://localhost:8080/json/api/v1/blog/1")
                        ),
                        JsonApiDSL.relationships(
                                JsonApiDSL.relation("author",
                                        JsonApiDSL.links(
                                                JsonApiDSL.attr("self", "http://localhost:8080/json/api/v1/blog/1/relationships/author"),
                                                JsonApiDSL.attr("related", "http://localhost:8080/json/api/v1/blog/1/author")
                                        ),
                                        JsonApiDSL.linkage(JsonApiDSL.type("author"), JsonApiDSL.id("1"))
                                ),
                                JsonApiDSL.relation("comments",
                                        JsonApiDSL.links(
                                                JsonApiDSL.attr("self", "http://localhost:8080/json/api/v1/blog/1/relationships/comments"),
                                                JsonApiDSL.attr("related", "http://localhost:8080/json/api/v1/blog/1/comments")
                                        )
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }
}
