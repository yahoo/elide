/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.visitor;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.schema.Schema;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

public class SplitFilterExpressionVisitorTest {

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

    private EntityDictionary entityDictionary;
    private Schema schema;
    private FilterExpressionVisitor<FilterConstraints> splitFilterExpressionVisitor;

    @BeforeMethod
    public void setupEntityDictionary() {
        entityDictionary = new EntityDictionary(Collections.emptyMap());
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(Player.class);
        schema = new Schema(PlayerStats.class, entityDictionary);
        splitFilterExpressionVisitor = new SplitFilterExpressionVisitor(schema);
    }

    @Test
    public void testVisitPredicate() {
        // predicate should be a WHERE
        Assert.assertTrue(splitFilterExpressionVisitor.visitPredicate(WHERE_PREDICATE).isPureWhere());
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitPredicate(WHERE_PREDICATE).getWhereExpression().toString(),
                "playerStats.id IN [foo]"
        );
        Assert.assertFalse(splitFilterExpressionVisitor.visitPredicate(WHERE_PREDICATE).isPureHaving());
        Assert.assertNull(splitFilterExpressionVisitor.visitPredicate(WHERE_PREDICATE).getHavingExpression());

        // predicate should be a HAVING
        Assert.assertTrue(splitFilterExpressionVisitor.visitPredicate(HAVING_PREDICATE).isPureHaving());
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitPredicate(HAVING_PREDICATE).getHavingExpression().toString(),
                "playerStats.highScore GT [99]"
        );
        Assert.assertFalse(splitFilterExpressionVisitor.visitPredicate(HAVING_PREDICATE).isPureWhere());
        Assert.assertNull(splitFilterExpressionVisitor.visitPredicate(HAVING_PREDICATE).getWhereExpression());
    }

    @Test
    public void testVisitAndExpression() {
        // pure-W AND pure-W
        AndFilterExpression filterExpression = new AndFilterExpression(WHERE_PREDICATE, WHERE_PREDICATE);
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getWhereExpression().toString(),
                "(playerStats.id IN [foo] AND playerStats.id IN [foo])"
        );
        Assert.assertNull(splitFilterExpressionVisitor.visitAndExpression(filterExpression).getHavingExpression());

        // pure-H AND pure-W
        filterExpression = new AndFilterExpression(HAVING_PREDICATE, WHERE_PREDICATE);
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getWhereExpression().toString(),
                "playerStats.id IN [foo]"
        );
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getHavingExpression().toString(),
                "playerStats.highScore GT [99]"
        );

        // pure-W AND pure-H
        filterExpression = new AndFilterExpression(WHERE_PREDICATE, HAVING_PREDICATE);
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getWhereExpression().toString(),
                "playerStats.id IN [foo]"
        );
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getHavingExpression().toString(),
                "playerStats.highScore GT [99]"
        );

        // non-pure case - H1 AND W1 AND H2
        AndFilterExpression and1 = new AndFilterExpression(HAVING_PREDICATE, WHERE_PREDICATE);
        AndFilterExpression and2 = new AndFilterExpression(and1, HAVING_PREDICATE);
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(and2).getWhereExpression().toString(),
                "playerStats.id IN [foo]"
        );
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(and2).getHavingExpression().toString(),
                "(playerStats.highScore GT [99] AND playerStats.highScore GT [99])"
        );

        // non-pure case - (H1 OR H2) AND W1
        OrFilterExpression or = new OrFilterExpression(HAVING_PREDICATE, HAVING_PREDICATE);
        AndFilterExpression and = new AndFilterExpression(or, WHERE_PREDICATE);
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(and).getWhereExpression().toString(),
                "playerStats.id IN [foo]"
        );
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitAndExpression(and).getHavingExpression().toString(),
                "(playerStats.highScore GT [99] OR playerStats.highScore GT [99])"
        );
    }

    @Test
    public void testVisitOrExpression() {
        // pure-W OR pure-W
        OrFilterExpression filterExpression = new OrFilterExpression(WHERE_PREDICATE, WHERE_PREDICATE);
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitOrExpression(filterExpression).getWhereExpression().toString(),
                "(playerStats.id IN [foo] OR playerStats.id IN [foo])"
        );
        Assert.assertNull(splitFilterExpressionVisitor.visitOrExpression(filterExpression).getHavingExpression());

        // H1 OR W1
        OrFilterExpression or = new OrFilterExpression(HAVING_PREDICATE, WHERE_PREDICATE);
        Assert.assertNull(splitFilterExpressionVisitor.visitOrExpression(or).getWhereExpression());
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitOrExpression(or).getHavingExpression().toString(),
                "(playerStats.highScore GT [99] OR playerStats.id IN [foo])"
        );

        // (W1 AND H1) OR W2
        AndFilterExpression and = new AndFilterExpression(WHERE_PREDICATE, HAVING_PREDICATE);
        or = new OrFilterExpression(and, WHERE_PREDICATE);
        Assert.assertNull(splitFilterExpressionVisitor.visitOrExpression(or).getWhereExpression());
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitOrExpression(or).getHavingExpression().toString(),
                "((playerStats.id IN [foo] AND playerStats.highScore GT [99]) OR playerStats.id IN [foo])"
        );
    }

    @Test
    public void testVisitNotExpression() {
        NotFilterExpression notExpression = new NotFilterExpression(
                new AndFilterExpression(WHERE_PREDICATE, HAVING_PREDICATE)
        );
        Assert.assertNull(splitFilterExpressionVisitor.visitNotExpression(notExpression).getWhereExpression());
        Assert.assertEquals(
                splitFilterExpressionVisitor.visitNotExpression(notExpression).getHavingExpression().toString(),
                "(playerStats.id NOT [foo] OR playerStats.highScore LE [99])"
        );

    }
}
