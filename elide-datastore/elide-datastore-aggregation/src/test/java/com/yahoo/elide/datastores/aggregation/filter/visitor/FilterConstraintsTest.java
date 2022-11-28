/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import example.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class FilterConstraintsTest {

    private static final FilterPredicate WHERE_PREDICATE = new FilterPredicate(
            new Path.PathElement(PlayerStats.class, String.class, "id"),
            Operator.IN,
            Collections.singletonList("foo")
    );
    private static final FilterPredicate HAVING_PREDICATE = new FilterPredicate(
            new Path.PathElement(PlayerStats.class, long.class, "highScore"),
            Operator.GT,
            Collections.singletonList(99)
    );

    @Test
    public void testPureHaving() {
        assertTrue(FilterConstraints.pureHaving(HAVING_PREDICATE).isPureHaving());
        assertFalse(FilterConstraints.pureHaving(HAVING_PREDICATE).isPureWhere());
        assertEquals(
                "playerStats.highScore GT [99]",
                FilterConstraints.pureHaving(HAVING_PREDICATE).getHavingExpression().toString()
        );
        assertNull(FilterConstraints.pureHaving(HAVING_PREDICATE).getWhereExpression());
    }

    @Test
    public void testPureWhere() {
        assertTrue(FilterConstraints.pureWhere(WHERE_PREDICATE).isPureWhere());
        assertFalse(FilterConstraints.pureWhere(WHERE_PREDICATE).isPureHaving());
        assertEquals(
                "playerStats.id IN [foo]",
                FilterConstraints.pureWhere(WHERE_PREDICATE).getWhereExpression().toString()
        );
        assertNull(FilterConstraints.pureWhere(WHERE_PREDICATE).getHavingExpression());
    }

    @Test
    public void testWithWhereAndHaving() {
        assertFalse(FilterConstraints.withWhereAndHaving(WHERE_PREDICATE, HAVING_PREDICATE).isPureWhere());
        assertFalse(FilterConstraints.withWhereAndHaving(WHERE_PREDICATE, HAVING_PREDICATE).isPureHaving());
        assertEquals(
                "playerStats.id IN [foo]",
                FilterConstraints.withWhereAndHaving(WHERE_PREDICATE, HAVING_PREDICATE).getWhereExpression().toString()
        );
        assertEquals(
                "playerStats.highScore GT [99]",
                FilterConstraints.withWhereAndHaving(WHERE_PREDICATE, HAVING_PREDICATE).getHavingExpression().toString()
        );
    }
}
