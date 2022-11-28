/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class QueryRunnerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "#abcd\nmutation",
            "#abcd\n\nmutation",
            "   #abcd\n\nmutation",
            "#abcd\n  #befd\n mutation",
            "mutation"
    })
    public void testIsMutation(String input) {
        assertTrue(QueryRunner.isMutation(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "#abcd\n  #befd\n query",
            "query",
            "QUERY",
            "MUTATION",
            ""
    })
    public void testIsNotMutation(String input) {
        assertFalse(QueryRunner.isMutation(input));
    }

    @Test
    public void testNullMutation() {
        assertFalse(QueryRunner.isMutation(null));
    }
}
