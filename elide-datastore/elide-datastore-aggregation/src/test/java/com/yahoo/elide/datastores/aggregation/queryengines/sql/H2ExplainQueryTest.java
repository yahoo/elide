/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class tests SQLQueryEngine.explain() with the H2 dialect.
 */
public class H2ExplainQueryTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        SQLUnitTest.init(SQLDialectFactory.getH2Dialect());
    }

//    TODO - Should this generate an error from the engine level?
//    @Test
//    public void testExplainNoMetricsOrDimensions() {
//        Query query = Query.builder()
//                .table(playerStatsTable)
//                .build();
//        String expectedQueryStr = "SELECT DISTINCT  " +
//                "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats";
//
//        compareQueryLists(expectedQueryStr, engine.explain(query));
//    }

    @Test
    public void testExplainWhereMetricsOnly() throws Exception {
        Query query = TestQuery.WHERE_METRICS_ONLY.getQuery();
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate) query.getWhereFilter()).getParameters();
        String expectedQueryStr =
                "SELECT highScore AS highScoreNoAgg,"
                        + "MIN(com_yahoo_elide_datastores_aggregation_example_PlayerStats.lowScore) AS lowScore "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE highScore > "
                        + params.get(0).getPlaceholder();
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainWhereDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.explain(TestQuery.WHERE_DIMS_ONLY.getQuery()));
    }

    @Test
    public void testExplainWhereMetricsAndDims() throws Exception {
        Query query = TestQuery.WHERE_METRICS_AND_DIMS.getQuery();
        AndFilterExpression andFilter = ((AndFilterExpression) query.getWhereFilter());
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate) andFilter.getRight()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
                        + "AND MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder()
                        + ") GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainWhereMetricsOrDims() throws Exception {
        Query query = TestQuery.WHERE_METRICS_OR_DIMS.getQuery();
        OrFilterExpression orFilter = ((OrFilterExpression) query.getWhereFilter());
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate) orFilter.getRight()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
                        + "OR MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder()
                        + ") GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainWhereMetricsAgg() throws Exception {
        Query query = TestQuery.WHERE_METRICS_AGGREGATION.getQuery();
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate) query.getWhereFilter()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        + "MIN(com_yahoo_elide_datastores_aggregation_example_PlayerStats.lowScore) AS lowScore "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder();
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingMetricsOnly() throws Exception {
        Query query = TestQuery.HAVING_METRICS_ONLY.getQuery();
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate) query.getHavingFilter()).getParameters();
        String expectedQueryStr =
                "SELECT highScore AS highScoreNoAgg,"
                        + "MIN(com_yahoo_elide_datastores_aggregation_example_PlayerStats.lowScore) AS lowScore "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "HAVING highScore > "
                        + params.get(0).getPlaceholder();
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "HAVING com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.explain(TestQuery.HAVING_DIMS_ONLY.getQuery()));
    }

    @Test
    public void testExplainHavingMetricsAndDims() throws Exception {
        Query query = TestQuery.HAVING_METRICS_AND_DIMS.getQuery();
        AndFilterExpression andFilter = ((AndFilterExpression) query.getHavingFilter());
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate) andFilter.getRight()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating "
                        + "HAVING (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
                        + "AND MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder() + ")";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingMetricsOrDims() throws Exception {
        Query query = TestQuery.HAVING_METRICS_OR_DIMS.getQuery();
        OrFilterExpression orFilter = ((OrFilterExpression) query.getHavingFilter());
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate) orFilter.getRight()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating "
                        + "HAVING (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
                        + "OR MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder() + ")";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainPagination() {
        String expectedQueryStr1 =
                "SELECT COUNT(DISTINCT(com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating, "
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate)) "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats";
        String expectedQueryStr2 =
                "SELECT MIN(com_yahoo_elide_datastores_aggregation_example_PlayerStats.lowScore) AS "
                        + "lowScore,com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS "
                        + "overallRating,PARSEDATETIME(FORMATDATETIME("
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS recordedDate FROM playerStats AS "
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats   "
                        + "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating, "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd')";
        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.PAGINATION_TOTAL.getQuery()));
    }

    @Test
    public void testExplainSortingAscending() {
        String expectedQueryStr =
                "SELECT highScore AS highScoreNoAgg "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats   "
                        + "ORDER BY highScore ASC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_ASC.getQuery()));
    }

    @Test
    public void testExplainSortingDecending() {
        String expectedQueryStr =
                "SELECT highScore AS highScoreNoAgg "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats   "
                        + "ORDER BY highScore DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_DESC.getQuery()));
    }

    @Test
    public void testExplainSortingByDimensionDesc() {
        String expectedQueryStr =
                "SELECT DISTINCT com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS "
                        + "overallRating FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "ORDER BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_DIM_DESC.getQuery()));
    }

    @Test
    public void testExplainSortingByMetricAndDimension() {
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) "
                        + "AS highScore,com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS "
                        + "overallRating FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating "
                        + "ORDER BY MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) DESC,"
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_AND_DIM_DESC.getQuery()));
    }

    @Test
    public void testExplainSelectFromSubquery() {
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStatsView.highScore) AS "
                        + "highScore FROM (SELECT stats.highScore, stats.player_id, c.name as countryName FROM "
                        + "playerStats AS stats LEFT JOIN countries AS c ON stats.country_id = c.id "
                        + "WHERE stats.overallRating = 'Great') AS "
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStatsView";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SUBQUERY.getQuery()));
    }

    @Test
    public void testExplainOrderByNotInSelect() {
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "ORDER BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.ORDER_BY_DIMENSION_NOT_IN_SELECT.getQuery()));
    }

    @Test
    public void testExplainComplicated() {
        Query query = TestQuery.COMPLICATED.getQuery();
        List<FilterPredicate.FilterParameter> whereParams = ((FilterPredicate) query.getWhereFilter()).getParameters();
        List<FilterPredicate.FilterParameter> havingParams = ((FilterPredicate) query.getHavingFilter()).getParameters();

        String expectedQueryStr1 =
                "SELECT COUNT(DISTINCT(com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating, "
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate)) "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "LEFT JOIN countries AS com_yahoo_elide_datastores_aggregation_example_PlayerStats_country "
                        + "ON com_yahoo_elide_datastores_aggregation_example_PlayerStats.country_id = "
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats_country.id "
                        + "WHERE highScore > " + whereParams.get(0).getPlaceholder() + " "
                        + "HAVING com_yahoo_elide_datastores_aggregation_example_PlayerStats_country.iso_code "
                        + "IN (" + havingParams.get(0).getPlaceholder() + ")";
        String expectedQueryStr2 =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating,"
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS recordedDate "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "LEFT JOIN countries AS com_yahoo_elide_datastores_aggregation_example_PlayerStats_country "
                        + "ON com_yahoo_elide_datastores_aggregation_example_PlayerStats.country_id = "
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats_country.id "
                        + "WHERE highScore > " + whereParams.get(0).getPlaceholder() + " "
                        + "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating, "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING com_yahoo_elide_datastores_aggregation_example_PlayerStats_country.iso_code "
                        + "IN (" + havingParams.get(0).getPlaceholder() + ") "
                        + "ORDER BY highScore DESC";
        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }
}
