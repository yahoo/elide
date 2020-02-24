/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;

import com.yahoo.elide.request.Sorting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class QueryValidatorTest extends SQLUnitTest {
    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
    }

    @Test
    public void testSortingOnId() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("id", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("id")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        Set<String> allFields = new HashSet<>(Arrays.asList("id", "overallRating", "lowScore"));
        QueryValidator validator = new QueryValidator(query, allFields, dictionary);

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, validator::validate);
        assertEquals("Invalid operation: Sorting on id field is not permitted", exception.getMessage());
    }

    @Test
    public void testSortingOnNotQueriedDimension() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryIsoCode", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        Set<String> allFields = new HashSet<>(Arrays.asList("overallRating", "lowScore"));
        QueryValidator validator = new QueryValidator(query, allFields, dictionary);

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, validator::validate);
        assertEquals("Invalid operation: Can not sort on countryIsoCode as it is not present in query", exception.getMessage());
    }

    @Test
    public void testSortingOnNotQueriedMetric() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("highScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        Set<String> allFields = new HashSet<>(Arrays.asList("overallRating", "lowScore"));
        QueryValidator validator = new QueryValidator(query, allFields, dictionary);

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, validator::validate);
        assertEquals("Invalid operation: Can not sort on highScore as it is not present in query", exception.getMessage());
    }

    @Test
    public void testHavingFilterPromotionUngroupedDimension() throws ParseException {
        FilterExpression originalFilter = filterParser.parseFilterExpression("countryIsoCode==USA,lowScore<45",
                PlayerStats.class, false);
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(playerStatsTable);
        FilterConstraints constraints = originalFilter.accept(visitor);
        FilterExpression whereFilter = constraints.getWhereExpression();
        FilterExpression havingFilter = constraints.getHavingExpression();

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .build();

        Set<String> allFields = new HashSet<>(Collections.singletonList("lowScore"));
        QueryValidator validator = new QueryValidator(query, allFields, dictionary);

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, validator::validate);
        assertEquals(
                "Invalid operation: Dimension field countryIsoCode must be grouped before filtering in having clause.",
                exception.getMessage());
    }

    @Test
    public void testHavingFilterNoAggregatedMetric() throws ParseException {
        FilterExpression originalFilter = filterParser.parseFilterExpression("lowScore<45", PlayerStats.class, false);
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(playerStatsTable);
        FilterConstraints constraints = originalFilter.accept(visitor);
        FilterExpression whereFilter = constraints.getWhereExpression();
        FilterExpression havingFilter = constraints.getHavingExpression();

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .build();

        Set<String> allFields = new HashSet<>(Collections.singletonList("highScore"));
        QueryValidator validator = new QueryValidator(query, allFields, dictionary);

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, validator::validate);
        assertEquals(
                "Invalid operation: Metric field lowScore must be aggregated before filtering in having clause.",
                exception.getMessage());
    }

    @Test
    public void testHavingFilterOnDimensionTable() throws ParseException {
        FilterExpression originalFilter = filterParser.parseFilterExpression("country.isoCode==USA,lowScore<45",
                PlayerStats.class, false);
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(playerStatsTable);
        FilterConstraints constraints = originalFilter.accept(visitor);
        FilterExpression whereFilter = constraints.getWhereExpression();
        FilterExpression havingFilter = constraints.getHavingExpression();

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .build();

        Set<String> allFields = new HashSet<>(Collections.singletonList("lowScore"));
        QueryValidator validator = new QueryValidator(query, allFields, dictionary);

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, validator::validate);
        assertEquals(
                "Invalid operation: Can not filter on relationship field [PlayerStats].country/[Country].isoCode in HAVING clause when querying table PlayerStats.",
                exception.getMessage());
    }
}
