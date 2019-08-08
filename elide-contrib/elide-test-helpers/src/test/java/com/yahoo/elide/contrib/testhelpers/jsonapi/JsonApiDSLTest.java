/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;
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
                                toOneRelation("author",
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
}
