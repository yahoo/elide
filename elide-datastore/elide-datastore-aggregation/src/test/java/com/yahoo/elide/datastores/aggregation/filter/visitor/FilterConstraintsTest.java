/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.visitor;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;

import org.testng.Assert;
import org.testng.annotations.Test;

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
        Assert.assertTrue(FilterConstraints.pureHaving(HAVING_PREDICATE).isPureHaving());
        Assert.assertFalse(FilterConstraints.pureHaving(HAVING_PREDICATE).isPureWhere());
        Assert.assertEquals(
                FilterConstraints.pureHaving(HAVING_PREDICATE).getHavingExpression().toString(),
                "playerStats.highScore GT [99]"
        );
        Assert.assertNull(FilterConstraints.pureHaving(HAVING_PREDICATE).getWhereExpression());
    }

    @Test
    public void testPureWhere() {
        Assert.assertTrue(FilterConstraints.pureWhere(WHERE_PREDICATE).isPureWhere());
        Assert.assertFalse(FilterConstraints.pureWhere(WHERE_PREDICATE).isPureHaving());
        Assert.assertEquals(
                FilterConstraints.pureWhere(WHERE_PREDICATE).getWhereExpression().toString(),
                "playerStats.id IN [foo]"
        );
        Assert.assertNull(FilterConstraints.pureWhere(WHERE_PREDICATE).getHavingExpression());
    }

    @Test
    public void testWithWhereAndHaving() {
        Assert.assertFalse(FilterConstraints.withWhereAndHaving(WHERE_PREDICATE, HAVING_PREDICATE).isPureWhere());
        Assert.assertFalse(FilterConstraints.withWhereAndHaving(WHERE_PREDICATE, HAVING_PREDICATE).isPureHaving());
        Assert.assertEquals(
                FilterConstraints.withWhereAndHaving(WHERE_PREDICATE, HAVING_PREDICATE).getWhereExpression().toString(),
                "playerStats.id IN [foo]"
        );
        Assert.assertEquals(
                FilterConstraints.withWhereAndHaving(WHERE_PREDICATE, HAVING_PREDICATE).getHavingExpression().toString(),
                "playerStats.highScore GT [99]"
        );
    }
}
