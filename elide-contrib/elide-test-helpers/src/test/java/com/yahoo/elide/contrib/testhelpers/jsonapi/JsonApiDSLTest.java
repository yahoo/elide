/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.elements.PatchOperationType.add;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Relation.TO_ONE;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

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

        assertEquals(actual, expected);
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

        assertEquals(actual, expected);
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

        assertEquals(actual, expected);
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

        assertEquals(actual, expected);

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

        assertEquals(actual, expected);
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

        assertEquals(actual, expected);
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

        assertEquals(actual, expected);
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


        assertEquals(actual, expected);
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


        assertEquals(actual, expected);
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


        assertEquals(actual, expected);
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


        assertEquals(actual, expected);
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

        assertEquals(actual, expected);
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

        assertEquals(actual, expected);
    }
}
