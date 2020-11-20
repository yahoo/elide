/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class tests a PostgreSQL generation of the engine.
 * This class only covers known differences/ use cases and will continue to grow as relavent differences are discovered.
 *
 */
public class PostgresExplainQueryTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        SQLUnitTest.init(SQLDialectFactory.getPostgresDialect());
    }

    @Test
    public void testExplainWhereDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "WHERE \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.explain(TestQuery.WHERE_DIMS_ONLY.getQuery()));
    }

    @Test
    public void testExplainWhereAnd() throws Exception {
        Query query = TestQuery.WHERE_AND.getQuery();
        String expectedQueryStr =
                "SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "LEFT JOIN \"countries\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\" "
                        + "ON \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"country_id\" = \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"id\" "
                        + "WHERE (\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" IS NOT NULL AND \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"iso_code\" IN (:XXX)) "
                        + " GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\"\n";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void textExplainWhereOr() throws Exception {
        Query query = TestQuery.WHERE_OR.getQuery();
        String expectedQueryStr =
                "SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "LEFT JOIN \"countries\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\" "
                        + "ON \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"country_id\" = \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"id\" "
                        + "WHERE (\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" IS NOT NULL OR \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"iso_code\" IN (:XXX)) "
                        + " GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\"\n";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingMetricsOnly() throws Exception {
        Query query = TestQuery.HAVING_METRICS_ONLY.getQuery();
        String expectedQueryStr =
                "SELECT MIN(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"lowScore\") AS \"lowScore\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "HAVING MIN(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"lowScore\") > :XXX";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "HAVING \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.explain(TestQuery.HAVING_DIMS_ONLY.getQuery()));
    }

    @Test
    public void testExplainHavingMetricsAndDims() throws Exception {
        Query query = TestQuery.HAVING_METRICS_AND_DIMS.getQuery();
        String expectedQueryStr =
                "SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" "
                        + "HAVING (\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" IS NOT NULL "
                        + "AND MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") > :XXX)";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingMetricsOrDims() throws Exception {
        Query query = TestQuery.HAVING_METRICS_OR_DIMS.getQuery();
        String expectedQueryStr =
                "SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" "
                        + "HAVING (\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" IS NOT NULL "
                        + "OR MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") > :XXX)";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    /*
     * This test validates that generateCountDistinctClause() is called in the PrestoDialect (same as default/H2).
     */
    @Test
    public void testExplainPagination() {
        String expectedQueryStr1 =
                "SELECT COUNT(DISTINCT(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\")) FROM "
                        + "\"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\"";
        String expectedQueryStr2 =
                "SELECT MIN(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"lowScore\") AS "
                        + "\"lowScore\",\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS "
                        + "\"overallRating\",PARSEDATETIME(FORMATDATETIME("
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS \"recordedDate\" FROM \"playerStats\" AS "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\"   "
                        + "GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') LIMIT 1 OFFSET 0";
        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.PAGINATION_TOTAL.getQuery()));
    }

    @Test
    public void testExplainSortingAscending() {
        String expectedQueryStr =
                "SELECT MIN(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"lowScore\") AS \"lowScore\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\"   "
                        + "ORDER BY \"lowScore\" ASC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_ASC.getQuery()));
    }

    @Test
    public void testExplainSortingDecending() {
        String expectedQueryStr =
                "SELECT MIN(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"lowScore\") AS \"lowScore\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\"   "
                        + "ORDER BY \"lowScore\" DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_DESC.getQuery()));
    }


    @Test
    public void testExplainSortingByDimensionDesc() {
        String expectedQueryStr =
                "SELECT DISTINCT \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS "
                        + "\"overallRating\" FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "ORDER BY \"overallRating\" DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_DIM_DESC.getQuery()));
    }

    @Test
    public void testExplainSortingByMetricAndDimension() {
        String expectedQueryStr =
                "SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") "
                        + "AS \"highScore\",\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS "
                        + "\"overallRating\" FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" "
                        + "ORDER BY \"highScore\" DESC,\"overallRating\" DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_AND_DIM_DESC.getQuery()));
    }


    @Test
    public void testExplainSelectFromSubquery() {
        String expectedQueryStr =
                "SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStatsView\".\"highScore\") AS "
                        + "\"highScore\" FROM (SELECT stats.highScore, stats.player_id, c.name as countryName FROM "
                        + "playerStats AS stats LEFT JOIN countries AS c ON stats.country_id = c.id "
                        + "WHERE stats.overallRating = 'Great') AS "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStatsView\"";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SUBQUERY.getQuery()));
    }

    /* TODO: Presto doesn't support this. To make this work, we'd need to push the ORDER BY field into the SELECT
             then drop it before returning the data.
    @Test
    public void testExplainOrderByNotInSelect() {
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "ORDER BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.ORDER_BY_DIMENSION_NOT_IN_SELECT.getQuery()));
    }
    */

    @Test
    public void testExplainComplicated() {
        Query query = TestQuery.COMPLICATED.getQuery();

        String expectedQueryStr1 =
                "SELECT COUNT(DISTINCT(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\")) "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "LEFT JOIN \"countries\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\" "
                        + "ON \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"country_id\" = "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"id\" "
                        + "WHERE \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"iso_code\" "
                        + "IN (:XXX) "
                        + "HAVING MIN(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"lowScore\") > :XXX";

        String expectedQueryStr2 =
                "SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS \"recordedDate\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "LEFT JOIN \"countries\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\" "
                        + "ON \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"country_id\" = "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"id\" "
                        + "WHERE \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"iso_code\" "
                        + "IN (:XXX) "
                        + "GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MIN(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"lowScore\") > :XXX "
                        + "ORDER BY MIN(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"lowScore\") DESC LIMIT 5 OFFSET 10";

        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricQuery() {
        Query query = TestQuery.NESTED_METRIC_QUERY.getQuery();

        String exptectedQueryStr =
                "SELECT AVG(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"highScore\") "
                        + "AS \"dailyAverageScorePerPeriod\",\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                        + "FROM (SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') AS \"recordedDate\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" GROUP BY "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') ) "
                        + "AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\" GROUP BY "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM')\n";

        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricWithHavingQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_HAVING_QUERY.getQuery();

        String exptectedQueryStr =
                "SELECT AVG(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"highScore\") "
                        + "AS \"dailyAverageScorePerPeriod\",\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                        + "FROM (SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') AS \"recordedDate\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" GROUP BY "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') ) "
                        + "AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\" GROUP BY "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM') "
                        + "HAVING AVG(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"highScore\") > :XXX\n";

        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricWithWhereQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_WHERE_QUERY.getQuery();

        String exptectedQueryStr =
                "SELECT AVG(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"highScore\") "
                        + "AS \"dailyAverageScorePerPeriod\",\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                        + "FROM (SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') AS \"recordedDate\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                        + "LEFT JOIN \"countries\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\" ON \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"country_id\" = \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"id\" "
                        + "WHERE \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_country\".\"iso_code\" IN (:XXX) "
                        + "GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') ) "
                        + "AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\" GROUP BY "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM')\n";

        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricWithPaginationQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_PAGINATION_QUERY.getQuery();

        String exptectedQueryStr1 = "SELECT COUNT(DISTINCT(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\", "
                + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\")) "
                + "FROM (SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') AS \"recordedDate\","
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" "
                + "GROUP BY \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') ) "
                + "AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\"\n";

        String exptectedQueryStr2 = "SELECT AVG(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"highScore\") "
                + "AS \"dailyAverageScorePerPeriod\",\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\" AS \"overallRating\","
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                + "FROM (SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') AS \"recordedDate\","
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" GROUP BY "
                + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') ) "
                + "AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\" GROUP BY "
                + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\", "
                + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM') "
                + "LIMIT 1 OFFSET 0\n";

        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(exptectedQueryStr1);
        expectedQueryList.add(exptectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricWithSortingQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_SORTING_QUERY.getQuery();

        String exptectedQueryStr =
                "SELECT AVG(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"highScore\") "
                        + "AS \"dailyAverageScorePerPeriod\",\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                        + "FROM (SELECT MAX(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') AS \"recordedDate\","
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') AS \"recordedMonth\" "
                        + "FROM \"playerStats\" AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats\" GROUP BY "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats\".\"recordedDate\", 'yyyy-MM'), 'yyyy-MM') ) "
                        + "AS \"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\" GROUP BY "
                        + "\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX\".\"recordedMonth\", 'yyyy-MM'), 'yyyy-MM') "
                        + "ORDER BY \"dailyAverageScorePerPeriod\" DESC,\"overallRating\" DESC";

        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }
}
