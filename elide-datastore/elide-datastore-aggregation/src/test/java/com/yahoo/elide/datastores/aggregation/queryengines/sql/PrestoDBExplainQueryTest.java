/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect.BACKTICK;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect.DOUBLE_QUOTE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class tests a Presto SQL generation of the engine.
 * This class only covers known differences/ use cases and will continue to grow as relavent differences are discovered.
 *
 * *** KEY ASSUMPTIONS ***
 *      * `parse_datetime(format_datetime())` shall be used instead of `parse_datetime(format_datetime())`
 *           when defining a datastore a real Presto environment.
 *        - PlayerStats.DAY_FORMAT provides an example of where this logic would have to be updated
 *        - com/yahoo/elide/datastores/aggregation/example/PlayerStats.java
 *
 *      * UDAFs (User-defined Aggregation Functions) such as MIN and MAX will be supported in the Hive environment
 *      * WHERE clause cannot contain aggregations, window functions or grouping operations in Presto
 * *** * * * * * * * * ***
 *
 */
public class PrestoDBExplainQueryTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        SQLUnitTest.init(SQLDialectFactory.getPrestoDBDialect());
    }

    @Test
    public void testExplainWhereDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT \"example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "WHERE \"example_PlayerStats\".\"overallRating\" IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.explain(TestQuery.WHERE_DIMS_ONLY.getQuery()));
    }

    @Test
    public void testExplainWhereAnd() throws Exception {
        Query query = TestQuery.WHERE_AND.getQuery();
        String expectedQueryStr =
                "SELECT MAX(\"example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "LEFT OUTER JOIN \"countries\" AS \"example_PlayerStats_country_XXX\" "
                        + "ON \"example_PlayerStats\".\"country_id\" = \"example_PlayerStats_country_XXX\".\"id\" "
                        + "WHERE (\"example_PlayerStats\".\"overallRating\" IS NOT NULL AND \"example_PlayerStats_country_XXX\".\"iso_code\" IN (:XXX)) "
                        + " GROUP BY \"example_PlayerStats\".\"overallRating\"\n";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void textExplainWhereOr() throws Exception {
        Query query = TestQuery.WHERE_OR.getQuery();
        String expectedQueryStr =
                "SELECT MAX(\"example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "LEFT OUTER JOIN \"countries\" AS \"example_PlayerStats_country_XXX\" "
                        + "ON \"example_PlayerStats\".\"country_id\" = \"example_PlayerStats_country_XXX\".\"id\" "
                        + "WHERE (\"example_PlayerStats\".\"overallRating\" IS NOT NULL OR \"example_PlayerStats_country_XXX\".\"iso_code\" IN (:XXX)) "
                        + " GROUP BY \"example_PlayerStats\".\"overallRating\"\n";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingMetricsOnly() throws Exception {
        Query query = TestQuery.HAVING_METRICS_ONLY.getQuery();
        String expectedQueryStr =
                "SELECT MIN(\"example_PlayerStats\".\"lowScore\") AS \"lowScore\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "HAVING MIN(\"example_PlayerStats\".\"lowScore\") > :XXX";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT \"example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "HAVING \"example_PlayerStats\".\"overallRating\" IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.explain(TestQuery.HAVING_DIMS_ONLY.getQuery()));
    }

    @Test
    public void testExplainHavingMetricsAndDims() throws Exception {
        Query query = TestQuery.HAVING_METRICS_AND_DIMS.getQuery();
        String expectedQueryStr =
                "SELECT MAX(\"example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "GROUP BY \"example_PlayerStats\".\"overallRating\" "
                        + "HAVING (\"example_PlayerStats\".\"overallRating\" IS NOT NULL "
                        + "AND MAX(\"example_PlayerStats\".\"highScore\") > :XXX)";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testExplainHavingMetricsOrDims() throws Exception {
        Query query = TestQuery.HAVING_METRICS_OR_DIMS.getQuery();
        String expectedQueryStr =
                "SELECT MAX(\"example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"example_PlayerStats\".\"overallRating\" AS \"overallRating\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "GROUP BY \"example_PlayerStats\".\"overallRating\" "
                        + "HAVING (\"example_PlayerStats\".\"overallRating\" IS NOT NULL "
                        + "OR MAX(\"example_PlayerStats\".\"highScore\") > :XXX)";
        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    /*
     * This test validates that generateCountDistinctClause() is called in the PrestoDialect (same as default/H2).
     */
    @Test
    public void testExplainPagination() {
        String expectedQueryStr1 = "SELECT COUNT(*) FROM "
                        + "(SELECT \"example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') FROM "
                        + "\"playerStats\" AS \"example_PlayerStats\" "
                        + "GROUP BY \"example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') ) AS \"pagination_subquery\"";
        String expectedQueryStr2 =
                "SELECT MIN(\"example_PlayerStats\".\"lowScore\") AS "
                        + "\"lowScore\",\"example_PlayerStats\".\"overallRating\" AS "
                        + "\"overallRating\",PARSEDATETIME(FORMATDATETIME("
                        + "\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS \"recordedDate\" FROM \"playerStats\" AS "
                        + "\"example_PlayerStats\"   "
                        + "GROUP BY \"example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') LIMIT 1";
        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.PAGINATION_TOTAL.getQuery()));
    }

    @Test
    public void testExplainSortingAscending() {
        String expectedQueryStr =
                "SELECT MIN(\"example_PlayerStats\".\"lowScore\") AS \"lowScore\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\"   "
                        + "ORDER BY \"lowScore\" ASC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_ASC.getQuery()));
    }

    @Test
    public void testExplainSortingDecending() {
        String expectedQueryStr =
                "SELECT MIN(\"example_PlayerStats\".\"lowScore\") AS \"lowScore\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\"   "
                        + "ORDER BY \"lowScore\" DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_DESC.getQuery()));
    }


    @Test
    public void testExplainSortingByDimensionDesc() {
        String expectedQueryStr =
                "SELECT DISTINCT \"example_PlayerStats\".\"overallRating\" AS "
                        + "\"overallRating\" FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "ORDER BY \"overallRating\" DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_DIM_DESC.getQuery()));
    }

    @Test
    public void testExplainSortingByMetricAndDimension() {
        String expectedQueryStr =
                "SELECT MAX(\"example_PlayerStats\".\"highScore\") "
                        + "AS \"highScore\",\"example_PlayerStats\".\"overallRating\" AS "
                        + "\"overallRating\" FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "GROUP BY \"example_PlayerStats\".\"overallRating\" "
                        + "ORDER BY \"highScore\" DESC,\"overallRating\" DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_AND_DIM_DESC.getQuery()));
    }


    @Test
    public void testExplainSelectFromSubquery() {
        String expectedQueryStr =
                "SELECT MAX(\"example_PlayerStatsView\".\"highScore\") AS "
                        + "\"highScore\" FROM (SELECT stats.highScore, stats.player_id, c.name as countryName FROM "
                        + "playerStats AS stats LEFT JOIN countries AS c ON stats.country_id = c.id "
                        + "WHERE stats.overallRating = 'Great' AND stats.highScore >= 0) AS "
                        + "\"example_PlayerStatsView\"";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SUBQUERY.getQuery()));
    }

    /* TODO: Presto doesn't support this. To make this work, we'd need to push the ORDER BY field into the SELECT
             then drop it before returning the data.
    @Test
    public void testExplainOrderByNotInSelect() {
        String expectedQueryStr =
                "SELECT MAX(example_PlayerStats.highScore) AS highScore "
                        + "FROM playerStats AS example_PlayerStats "
                        + "ORDER BY example_PlayerStats.overallRating DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.ORDER_BY_DIMENSION_NOT_IN_SELECT.getQuery()));
    }
    */

    @Test
    public void testExplainComplicated() {
        Query query = TestQuery.COMPLICATED.getQuery();

        String expectedQueryStr1 = "SELECT COUNT(*) FROM "
                        + "(SELECT \"example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "LEFT OUTER JOIN \"countries\" AS \"example_PlayerStats_country_XXX\" "
                        + "ON \"example_PlayerStats\".\"country_id\" = "
                        + "\"example_PlayerStats_country_XXX\".\"id\" "
                        + "WHERE \"example_PlayerStats_country_XXX\".\"iso_code\" "
                        + "IN (:XXX) "
                        + "GROUP BY \"example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MIN(\"example_PlayerStats\".\"lowScore\") > :XXX ) AS \"pagination_subquery\"";

        String expectedQueryStr2 =
                "SELECT MAX(\"example_PlayerStats\".\"highScore\") AS \"highScore\","
                        + "\"example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS \"recordedDate\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "LEFT OUTER JOIN \"countries\" AS \"example_PlayerStats_country_XXX\" "
                        + "ON \"example_PlayerStats\".\"country_id\" = "
                        + "\"example_PlayerStats_country_XXX\".\"id\" "
                        + "WHERE \"example_PlayerStats_country_XXX\".\"iso_code\" "
                        + "IN (:XXX) "
                        + "GROUP BY \"example_PlayerStats\".\"overallRating\", "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MIN(\"example_PlayerStats\".\"lowScore\") > :XXX "
                        + "ORDER BY MIN(\"example_PlayerStats\".\"lowScore\") DESC LIMIT 5";

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricQuery() {
        Query query = TestQuery.NESTED_METRIC_QUERY.getQuery();

        String exptectedQueryStr = getExpectedNestedMetricQuery().replace(BACKTICK, DOUBLE_QUOTE);

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(TestQuery.NESTED_METRIC_QUERY.getQuery());
    }

    @Test
    public void testNestedMetricWithHavingQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_HAVING_QUERY.getQuery();

        String exptectedQueryStr = getExpectedNestedMetricWithHavingQuery().replace(BACKTICK, DOUBLE_QUOTE);

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricWithWhereQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_WHERE_QUERY.getQuery();

        String exptectedQueryStr = getExpectedNestedMetricWithWhereQuery().replace(BACKTICK, DOUBLE_QUOTE);

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricWithPaginationQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_PAGINATION_QUERY.getQuery();
        String exptectedQueryStr1 = "SELECT COUNT(*) FROM "
                + "(SELECT \"example_PlayerStats_XXX\".\"overallRating\", "
                + "\"example_PlayerStats_XXX\".\"recordedDate\" "
                + "FROM (SELECT MAX(\"example_PlayerStats\".\"highScore\") AS \"highScore\","
                + "\"example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') AS \"recordedDate_XXX\","
                + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-01'), 'yyyy-MM-dd') AS \"recordedDate\" "
                + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                + "GROUP BY \"example_PlayerStats\".\"overallRating\", "
                + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-01'), 'yyyy-MM-dd') ) "
                + "AS \"example_PlayerStats_XXX\" "
                + "GROUP BY \"example_PlayerStats_XXX\".\"overallRating\", "
                + "\"example_PlayerStats_XXX\".\"recordedDate\" ) AS \"pagination_subquery\"\n";

        String exptectedQueryStr2 = "SELECT AVG(\"example_PlayerStats_XXX\".\"highScore\") "
                + "AS \"dailyAverageScorePerPeriod\",\"example_PlayerStats_XXX\".\"overallRating\" AS \"overallRating\","
                + "\"example_PlayerStats_XXX\".\"recordedDate\" AS \"recordedDate\" "
                + "FROM (SELECT MAX(\"example_PlayerStats\".\"highScore\") AS \"highScore\","
                + "\"example_PlayerStats\".\"overallRating\" AS \"overallRating\","
                + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd') AS \"recordedDate_XXX\","
                + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-01'), 'yyyy-MM-dd') AS \"recordedDate\" "
                + "FROM \"playerStats\" AS \"example_PlayerStats\" GROUP BY "
                + "\"example_PlayerStats\".\"overallRating\", "
                + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(\"example_PlayerStats\".\"recordedDate\", 'yyyy-MM-01'), 'yyyy-MM-dd') ) "
                + "AS \"example_PlayerStats_XXX\" GROUP BY "
                + "\"example_PlayerStats_XXX\".\"overallRating\", "
                + "\"example_PlayerStats_XXX\".\"recordedDate\" "
                + "LIMIT 1\n";

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr1);
        expectedQueryList.add(exptectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricWithSortingQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_SORTING_QUERY.getQuery();

        String exptectedQueryStr = getExpectedNestedMetricWithSortingQuery(true).replace(BACKTICK, DOUBLE_QUOTE);

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));
    }

    @Test
    public void testNestedMetricWithAliasesQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_ALIASES_QUERY.getQuery();

        String queryStr = engine.explain(query).get(0);
        queryStr = repeatedWhitespacePattern.matcher(queryStr).replaceAll(" ");
        queryStr = queryStr.replaceAll(":[a-zA-Z0-9_]+", ":XXX");
        queryStr = queryStr.replaceAll("PlayerStats_\\d+", "PlayerStats_XXX");
        queryStr = queryStr.replaceAll("PlayerStats_country_\\d+", "PlayerStats_country_XXX");

        String expectedStr = getExpectedNestedMetricWithAliasesSQL(true).replace(BACKTICK, DOUBLE_QUOTE);
        assertEquals(expectedStr, queryStr);
    }

    @Test
    public void testWhereWithArguments() {
        Query query = TestQuery.WHERE_WITH_ARGUMENTS.getQuery();

        String queryStr = engine.explain(query).get(0);
        queryStr = repeatedWhitespacePattern.matcher(queryStr).replaceAll(" ");

        String expectedStr = getExpectedWhereWithArgumentsSQL().replace(BACKTICK, DOUBLE_QUOTE);
        assertEquals(expectedStr, queryStr);
    }

    @Test
    public void testLeftJoin() throws Exception {
        Query query = TestQuery.LEFT_JOIN.getQuery();

        String expectedQueryStr =
                        "SELECT DISTINCT \"example_VideoGame_player_XXX\".\"name\" AS \"playerName\" FROM \"videoGames\" AS \"example_VideoGame\""
                                        + " LEFT OUTER JOIN \"players\" AS \"example_VideoGame_player_XXX\" ON \"example_VideoGame\".\"player_id\""
                                        + " = \"example_VideoGame_player_XXX\".\"id\"";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testInnerJoin() throws Exception {
        Query query = TestQuery.INNER_JOIN.getQuery();

        String expectedQueryStr =
                        "SELECT DISTINCT \"example_VideoGame_playerInnerJoin_XXX\".\"name\" AS \"playerNameInnerJoin\" FROM \"videoGames\" AS \"example_VideoGame\""
                                        + " INNER JOIN \"players\" AS \"example_VideoGame_playerInnerJoin_XXX\" ON \"example_VideoGame\".\"player_id\""
                                        + " = \"example_VideoGame_playerInnerJoin_XXX\".\"id\"";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testCrossJoin() throws Exception {
        Query query = TestQuery.CROSS_JOIN.getQuery();

        String expectedQueryStr =
                        "SELECT DISTINCT \"example_VideoGame_playerCrossJoin_XXX\".\"name\" AS \"playerNameCrossJoin\" FROM \"videoGames\" AS \"example_VideoGame\""
                                        + " CROSS JOIN \"players\" AS \"example_VideoGame_playerCrossJoin_XXX\"";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testJoinWithMetrics() throws Exception {
        Query query = TestQuery.METRIC_JOIN.getQuery();

        String expectedQueryStr = "SELECT "
                + "MAX(\"example_VideoGame_playerStats_XXX\".\"highScore\") / SUM(\"example_VideoGame\".\"timeSpent\") AS \"normalizedHighScore\" "
                + "FROM \"videoGames\" AS \"example_VideoGame\" "
                + "LEFT OUTER JOIN \"playerStats\" AS \"example_VideoGame_playerStats_XXX\" "
                + "ON \"example_VideoGame\".\"player_id\" = \"example_VideoGame_playerStats_XXX\".\"id\"";

        compareQueryLists(expectedQueryStr, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testPaginationMetricsOnly() throws Exception {
        // pagination query should be empty since there is no dimension projection
        Query query = TestQuery.PAGINATION_METRIC_ONLY.getQuery();
        String expectedQueryStr =
                "SELECT MIN(\"example_PlayerStats\".\"lowScore\") AS \"lowScore\" "
                        + "FROM \"playerStats\" AS \"example_PlayerStats\" "
                        + "LIMIT 5\n";
        compareQueryLists(expectedQueryStr, engine.explain(query));

        testQueryExecution(TestQuery.PAGINATION_METRIC_ONLY.getQuery());
    }
}
