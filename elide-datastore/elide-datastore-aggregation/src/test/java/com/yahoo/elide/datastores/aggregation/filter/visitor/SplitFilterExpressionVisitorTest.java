/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.visitor;

import static com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.DEFAULT_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Namespace;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import example.Player;
import example.PlayerStats;
import example.dimensions.Country;
import example.dimensions.SubCountry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class SplitFilterExpressionVisitorTest {

    private static final FilterPredicate WHERE_PREDICATE = new FilterPredicate(
            new Path.PathElement(PlayerStats.class, String.class, "overallRating"),
            Operator.IN,
            Collections.singletonList("foo")
    );
    private static final FilterPredicate HAVING_PREDICATE = new FilterPredicate(
            new Path.PathElement(PlayerStats.class, long.class, "highScore"),
            Operator.GT,
            Collections.singletonList(99)
    );

    private static FilterExpressionVisitor<FilterConstraints> splitFilterExpressionVisitor;

    @BeforeAll
    public static void setupEntityDictionary() {
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(SubCountry.class);
        entityDictionary.bindEntity(Player.class);

        Namespace namespace = new Namespace(DEFAULT_NAMESPACE);
        Table table = new SQLTable(namespace, ClassType.of(PlayerStats.class), entityDictionary);
        splitFilterExpressionVisitor = new SplitFilterExpressionVisitor(table);
    }

    @Test
    public void testVisitPredicate() {
        // predicate should be a WHERE
        assertTrue(splitFilterExpressionVisitor.visitPredicate(WHERE_PREDICATE).isPureWhere());
        assertEquals(
                "playerStats.overallRating IN [foo]",
                splitFilterExpressionVisitor.visitPredicate(WHERE_PREDICATE).getWhereExpression().toString()
        );
        assertFalse(splitFilterExpressionVisitor.visitPredicate(WHERE_PREDICATE).isPureHaving());
        assertNull(splitFilterExpressionVisitor.visitPredicate(WHERE_PREDICATE).getHavingExpression());

        // predicate should be a HAVING
        assertTrue(splitFilterExpressionVisitor.visitPredicate(HAVING_PREDICATE).isPureHaving());
        assertEquals(
                "playerStats.highScore GT [99]",
                splitFilterExpressionVisitor.visitPredicate(HAVING_PREDICATE).getHavingExpression().toString()
        );
        assertFalse(splitFilterExpressionVisitor.visitPredicate(HAVING_PREDICATE).isPureWhere());
        assertNull(splitFilterExpressionVisitor.visitPredicate(HAVING_PREDICATE).getWhereExpression());
    }

    @Test
    public void testVisitAndExpression() {
        // pure-W AND pure-W
        AndFilterExpression filterExpression = new AndFilterExpression(WHERE_PREDICATE, WHERE_PREDICATE);
        assertEquals(
                "(playerStats.overallRating IN [foo] AND playerStats.overallRating IN [foo])",
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getWhereExpression().toString()
        );
        assertNull(splitFilterExpressionVisitor.visitAndExpression(filterExpression).getHavingExpression());

        // pure-H AND pure-W
        filterExpression = new AndFilterExpression(HAVING_PREDICATE, WHERE_PREDICATE);
        assertEquals(
                "playerStats.overallRating IN [foo]",
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getWhereExpression().toString()
        );
        assertEquals(
                "playerStats.highScore GT [99]",
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getHavingExpression().toString()
        );

        // pure-W AND pure-H
        filterExpression = new AndFilterExpression(WHERE_PREDICATE, HAVING_PREDICATE);
        assertEquals(
                "playerStats.overallRating IN [foo]",
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getWhereExpression().toString()
        );
        assertEquals(
                "playerStats.highScore GT [99]",
                splitFilterExpressionVisitor.visitAndExpression(filterExpression).getHavingExpression().toString()
        );

        // non-pure case - H1 AND W1 AND H2
        AndFilterExpression and1 = new AndFilterExpression(HAVING_PREDICATE, WHERE_PREDICATE);
        AndFilterExpression and2 = new AndFilterExpression(and1, HAVING_PREDICATE);
        assertEquals(
                "playerStats.overallRating IN [foo]",
                splitFilterExpressionVisitor.visitAndExpression(and2).getWhereExpression().toString()
        );
        assertEquals(
                "(playerStats.highScore GT [99] AND playerStats.highScore GT [99])",
                splitFilterExpressionVisitor.visitAndExpression(and2).getHavingExpression().toString()
        );

        // non-pure case - (H1 OR H2) AND W1
        OrFilterExpression or = new OrFilterExpression(HAVING_PREDICATE, HAVING_PREDICATE);
        AndFilterExpression and = new AndFilterExpression(or, WHERE_PREDICATE);
        assertEquals(
                "playerStats.overallRating IN [foo]",
                splitFilterExpressionVisitor.visitAndExpression(and).getWhereExpression().toString()
        );
        assertEquals(
                "(playerStats.highScore GT [99] OR playerStats.highScore GT [99])",
                splitFilterExpressionVisitor.visitAndExpression(and).getHavingExpression().toString()
        );
    }

    @Test
    public void testVisitOrExpression() {
        // pure-W OR pure-W
        OrFilterExpression filterExpression = new OrFilterExpression(WHERE_PREDICATE, WHERE_PREDICATE);
        assertEquals(
                "(playerStats.overallRating IN [foo] OR playerStats.overallRating IN [foo])",
                splitFilterExpressionVisitor.visitOrExpression(filterExpression).getWhereExpression().toString()
        );
        assertNull(splitFilterExpressionVisitor.visitOrExpression(filterExpression).getHavingExpression());

        // H1 OR W1
        OrFilterExpression or = new OrFilterExpression(HAVING_PREDICATE, WHERE_PREDICATE);
        assertNull(splitFilterExpressionVisitor.visitOrExpression(or).getWhereExpression());
        assertEquals(
                "(playerStats.highScore GT [99] OR playerStats.overallRating IN [foo])",
                splitFilterExpressionVisitor.visitOrExpression(or).getHavingExpression().toString()
        );

        // (W1 AND H1) OR W2
        AndFilterExpression and = new AndFilterExpression(WHERE_PREDICATE, HAVING_PREDICATE);
        or = new OrFilterExpression(and, WHERE_PREDICATE);
        assertNull(splitFilterExpressionVisitor.visitOrExpression(or).getWhereExpression());
        assertEquals(
                "((playerStats.overallRating IN [foo] AND playerStats.highScore GT [99]) OR playerStats.overallRating IN [foo])",
                splitFilterExpressionVisitor.visitOrExpression(or).getHavingExpression().toString()
        );
    }

    @Test
    public void testVisitNotExpression() {
        NotFilterExpression notExpression = new NotFilterExpression(
                new AndFilterExpression(WHERE_PREDICATE, HAVING_PREDICATE)
        );
        assertNull(splitFilterExpressionVisitor.visitNotExpression(notExpression).getWhereExpression());
        assertEquals(
                "(playerStats.overallRating NOT [foo] OR playerStats.highScore LE [99])",
                splitFilterExpressionVisitor.visitNotExpression(notExpression).getHavingExpression().toString()
        );

    }
}
