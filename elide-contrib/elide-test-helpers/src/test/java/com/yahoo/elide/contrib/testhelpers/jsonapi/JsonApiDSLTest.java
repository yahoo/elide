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
        String expected = "{\"data\":{\"id\":1,\"type\":\"blog\"}}";

        String actual = data(
                resource(
                        type("blog"),
                        id(1)
                )
        ).toJSON();

        assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithAttributes() {
        String expected = "{\"data\":{\"id\":\"1\",\"type\":\"blog\",\""
                + "attributes\":{\"title\":\"Why You Should use Elide\",\"date\":\"2019-01-01\"}}}";

        String actual = data(
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
        String expected = "{\"data\":{\"id\":\"1\",\"type\":\"blog\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{\"author\":{\"data\":[{\"id\":\"1\",\"type\":\"author\"}]}}}}";

        String actual = data(
                resource(
                        type("blog"),
                        id("1"),
                        attributes(
                                attr("title", "title")
                        ),
                        relationships(
                                relation("author",
                                        linkage(type("author"), id("1"))
                                )
                        )
                )
        ).toJSON();

        assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithOneToManyRelationship() {
        String expected = "{\"data\":{\"id\":\"1\",\"type\":\"blog\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{\"comments\":{"
                + "\"data\":[{\"id\":\"1\",\"type\":\"comment\"},{\"id\":\"2\",\"type\":\"comment\"}]}}}}";

        String actual = data(
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
    }

    @Test
    public void verifyRequestWithManyRelationships() {
        String expected = "{\"data\":{\"id\":\"1\",\"type\":\"blog\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{"
                + "\"author\":{\"data\":[{\"id\":\"1\",\"type\":\"author\"}]},"
                + "\"comments\":{\"data\":[{\"id\":\"2\",\"type\":\"comment\"}]}}}}";

        String actual = data(
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
