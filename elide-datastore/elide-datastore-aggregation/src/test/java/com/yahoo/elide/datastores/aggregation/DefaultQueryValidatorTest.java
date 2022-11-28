/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import example.PlayerStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DefaultQueryValidatorTest extends SQLUnitTest {
    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
    }

    @Test
    public void testInvalidTableArgument() {
        SQLTable source = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);


        Map<String, Argument> tableArguments = new HashMap<>();
        tableArguments.put("rating", Argument.builder().name("rating").value("SELECT * FROM FOO;").build());

        Map<String, Argument> columnArguments = new HashMap<>();
        columnArguments.put("format", Argument.builder().name("format").value("lower").build());

        Query query = Query.builder()
                .source(source)
                .arguments(tableArguments)
                .metricProjection(source.getMetricProjection("highScore"))
                .dimensionProjection(source.getDimensionProjection("countryName", columnArguments))
                .build();

        validateQuery(query, "Invalid operation: Argument 'rating' for table 'playerStatsView' has an invalid value: SELECT * FROM FOO;");
    }

    @Test
    public void testValidTableArgument() {
        SQLTable source = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);

        Map<String, Argument> tableArguments = new HashMap<>();
        tableArguments.put("rating", Argument.builder().name("rating").value("Terrible").build());

        Map<String, Argument> columnArguments = new HashMap<>();
        columnArguments.put("format", Argument.builder().name("format").value("lower").build());

        Query query = Query.builder()
                .source(source)
                .arguments(tableArguments)
                .metricProjection(source.getMetricProjection("highScore"))
                .dimensionProjection(source.getDimensionProjection("countryName", columnArguments))
                .build();

        validateQueryDoesNotThrow(query);
    }

    @Test
    public void testInvalidColumnArgument() {
        SQLTable source = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);

        Map<String, Argument> argumentMap = new HashMap<>();
        argumentMap.put("format", Argument.builder().name("format").value(";").build());

        Query query = Query.builder()
                .source(source)
                .metricProjection(source.getMetricProjection("highScore"))
                .dimensionProjection(source.getDimensionProjection("countryName", "countryName", argumentMap))
                .build();

        validateQuery(query, "Invalid operation: Argument 'format' for column 'countryName' must match one of these values: [lower, upper]");
    }

    @Test
    public void testValidColumnArgument() {
        SQLTable source = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);

        Map<String, Argument> tableArguments = new HashMap<>();
        tableArguments.put("rating", Argument.builder().name("rating").value("Terrible").build());

        Map<String, Argument> argumentMap = new HashMap<>();
        argumentMap.put("format", Argument.builder().name("format").value("lower").build());

        Query query = Query.builder()
                .source(source)
                .arguments(tableArguments)
                .metricProjection(source.getMetricProjection("highScore"))
                .dimensionProjection(source.getDimensionProjection("countryName", "countryName", argumentMap))
                .build();

        validateQueryDoesNotThrow(query);
    }

    @Test
    public void testQueryingByIdAlone() {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("id"))
                .build();

        validateQuery(query, "Invalid operation: Cannot query a table only by ID");
    }

    @Test
    public void testSortingOnId() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("id", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .metricProjection(playerStatsTable.getMetricProjection("id"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        validateQuery(query, "Invalid operation: Sorting on id field is not permitted");
    }

    @Test
    public void testSortingOnNotQueriedDimension() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryIsoCode", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        validateQuery(query, "Invalid operation: Can not sort on countryIsoCode as it is not present in query");
    }

    @Test
    public void testSortingOnNotQueriedMetric() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("highScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        validateQuery(query, "Invalid operation: Can not sort on highScore as it is not present in query");
    }

    @Test
    public void testHavingFilterPromotionUngroupedDimension() throws ParseException {
        FilterExpression originalFilter = filterParser.parseFilterExpression("countryIsoCode==USA,lowScore<45",
                playerStatsType, false);
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(playerStatsTable);
        FilterConstraints constraints = originalFilter.accept(visitor);
        FilterExpression whereFilter = constraints.getWhereExpression();
        FilterExpression havingFilter = constraints.getHavingExpression();

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .build();

        validateQuery(query, "Invalid operation: Post aggregation filtering on 'countryIsoCode' requires the field to be projected in the response");
    }

    @Test
    public void testInvalidColumnValueInFilter() throws ParseException {
        FilterExpression originalFilter = filterParser.parseFilterExpression("countryIsoCode==Invalid,lowScore<45",
                playerStatsType, false);
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(playerStatsTable);
        FilterConstraints constraints = originalFilter.accept(visitor);
        FilterExpression whereFilter = constraints.getWhereExpression();
        FilterExpression havingFilter = constraints.getHavingExpression();

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("countryIsoCode"))
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .build();

        validateQuery(query, "Invalid operation: Column 'countryIsoCode' values must match one of these values: [HKG, USA]");
    }

    @Test
    public void testValidRegexColumnValueInFilter() throws ParseException {
        FilterExpression originalFilter = filterParser.parseFilterExpression("countryIsoCode=in=('*H'),lowScore<45",
                playerStatsType, false);
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(playerStatsTable);
        FilterConstraints constraints = originalFilter.accept(visitor);
        FilterExpression whereFilter = constraints.getWhereExpression();
        FilterExpression havingFilter = constraints.getHavingExpression();

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("countryIsoCode"))
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .build();

        validateQueryDoesNotThrow(query);
    }

    @Test
    public void testHavingFilterOnDimensionTable() throws ParseException {
        FilterExpression originalFilter = filterParser.parseFilterExpression("country.isoCode==USA,lowScore<45",
                playerStatsType, false);
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(playerStatsTable);
        FilterConstraints constraints = originalFilter.accept(visitor);
        FilterExpression whereFilter = constraints.getWhereExpression();
        FilterExpression havingFilter = constraints.getHavingExpression();

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .build();

        validateQuery(query, "Invalid operation: Relationship traversal not supported for analytic queries.");
    }

    @Test
    public void testHavingFilterMismatchedWithProjection() throws ParseException {
        SQLTable source = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);

        FilterExpression havingFilter = filterParser.parseFilterExpression("countryName[format:upper]==USA",
                playerStatsViewType, false);

        Query query = Query.builder()
                .source(source)
                .dimensionProjection(source.getDimensionProjection("countryName"))
                .havingFilter(havingFilter)
                .build();

        validateQuery(query, "Invalid operation: Post aggregation filtering on 'countryName' requires the field to be projected in the response with matching arguments");
    }

    @Test
    public void testHavingFilterMatchesProjection() throws ParseException {
        SQLTable source = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);

        Map<String, Argument> tableArguments = new HashMap<>();
        tableArguments.put("rating", Argument.builder().name("rating").value("Terrible").build());

        Map<String, Argument> arguments = new HashMap<>();
        arguments.put("format", Argument.builder().name("format").value("lower").build());

        FilterExpression havingFilter = filterParser.parseFilterExpression("countryName[format:lower]==usa",
                playerStatsViewType, false);

        Query query = Query.builder()
                .source(source)
                .arguments(tableArguments)
                .dimensionProjection(source.getDimensionProjection("countryName", arguments))
                .havingFilter(havingFilter)
                .build();

        validateQueryDoesNotThrow(query);
    }

    @Test
    public void testMissingRequiredParameterInProjection() {
        SQLTable source = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);

        Query query = Query.builder()
                .source(source)
                .metricProjection(source.getMetricProjection("highScore"))
                .dimensionProjection(source.getDimensionProjection("countryName"))
                .build();

        validateQuery(query, "Invalid operation: Argument 'format' for column 'countryName' is required");
    }

    @Test
    public void testMissingRequiredParameterInFilter() throws ParseException {
        SQLTable source = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);

        FilterExpression havingFilter = filterParser.parseFilterExpression("countryName==usa",
                playerStatsViewType, false);

        Query query = Query.builder()
                .source(source)
                .dimensionProjection(source.getDimensionProjection("countryName"))
                .havingFilter(havingFilter)
                .build();

        validateQuery(query, "Invalid operation: Argument 'format' for column 'countryName' is required");
    }

    private void validateQuery(Query query, String message) {
        DefaultQueryValidator validator = new DefaultQueryValidator(dictionary);

        InvalidOperationException exception = assertThrows(InvalidOperationException.class,
                () -> validator.validate(query));
        assertEquals(message, exception.getMessage());
    }

    private void validateQueryDoesNotThrow(Query query) {
        DefaultQueryValidator validator = new DefaultQueryValidator(dictionary);

        assertDoesNotThrow(() -> validator.validate(query));
    }
}
