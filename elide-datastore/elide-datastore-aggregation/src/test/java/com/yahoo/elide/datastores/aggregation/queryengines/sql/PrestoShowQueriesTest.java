package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

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
public class PrestoShowQueriesTest extends SQLUnitTest{
    private static Table playerStatsViewTable;


    @BeforeAll
    public static void init() {
        SQLUnitTest.init(new SQLDialectFactory().getPrestoDialect());

        playerStatsViewTable = engine.getTable("playerStatsView");
    }

    @Test
    public void testShowQueriesWhereMetricsOnly() throws Exception {
        Query query = testQueries.get(TestQueryName.WHERE_METRICS_ONLY);
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate)query.getWhereFilter()).getParameters();
        String expectedQueryStr =
                "SELECT highScore AS highScoreNoAgg,"
                        + "MIN(com_yahoo_elide_datastores_aggregation_example_PlayerStats.lowScore) AS lowScore "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE highScore > "
                        + params.get(0).getPlaceholder();
        compareQueryLists(expectedQueryStr, engine.showQueries(query));
    }

    @Test
    public void testShowQueriesWhereDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.showQueries(testQueries.get(TestQueryName.WHERE_DIMS_ONLY)));
    }

    @Test
    public void testShowQueriesWhereMetricsAndDims() throws Exception {
        Query query = testQueries.get(TestQueryName.WHERE_METRICS_AND_DIMS);
        AndFilterExpression andFilter = ((AndFilterExpression)query.getWhereFilter());
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate)andFilter.getRight()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        +"com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
                        + "AND MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder()
                        + ") GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating";
        compareQueryLists(expectedQueryStr, engine.showQueries(query));
    }

    @Test
    public void testShowQueriesWhereMetricsOrDims() throws Exception {
        Query query = testQueries.get(TestQueryName.WHERE_METRICS_OR_DIMS);
        OrFilterExpression orFilter = ((OrFilterExpression)query.getWhereFilter());
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate)orFilter.getRight()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        +"com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "WHERE (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
                        + "OR MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder()
                        + ") GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating";
        compareQueryLists(expectedQueryStr, engine.showQueries(query));
    }

    /**
     * // TODO: UDFS / Aggregations not allowed in where/groupby clause
     @Test
     public void testShowQueriesWhereMetricsAggregation() throws Exception {
     Query query = testQueries.get(TestQueryName.WHERE_METRICS_AGGREGATION);
     OrFilterExpression orFilter = ((OrFilterExpression)query.getWhereFilter());
     List<FilterPredicate.FilterParameter> params = ((FilterPredicate)orFilter.getRight()).getParameters();
     String expectedQueryStr =
     "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
     +"com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
     + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
     + "WHERE (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
     + "OR MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
     + params.get(0).getPlaceholder()
     + ") GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating";
     compareQueryLists(expectedQueryStr, engine.showQueries(query));
     }
     */

    @Test
    public void testShowQueriesHavingMetricsOnly() throws Exception {
        Query query = testQueries.get(TestQueryName.HAVING_METRICS_ONLY);
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate)query.getHavingFilter()).getParameters();
        String expectedQueryStr =
                "SELECT highScore AS highScoreNoAgg,"
                        + "MIN(com_yahoo_elide_datastores_aggregation_example_PlayerStats.lowScore) AS lowScore "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "HAVING highScore > "
                        + params.get(0).getPlaceholder();
        compareQueryLists(expectedQueryStr, engine.showQueries(query));
    }

    @Test
    public void testShowQueriesHavingDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "HAVING com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.showQueries(testQueries.get(TestQueryName.HAVING_DIMS_ONLY)));
    }

    @Test
    public void testShowQueriesHavingMetricsAndDims() throws Exception {
        Query query = testQueries.get(TestQueryName.HAVING_METRICS_AND_DIMS);
        AndFilterExpression andFilter = ((AndFilterExpression)query.getHavingFilter());
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate)andFilter.getRight()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        +"com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating "
                        + "HAVING (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
                        + "AND MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder() + ")";
        compareQueryLists(expectedQueryStr, engine.showQueries(query));
    }

    @Test
    public void testShowQueriesHavingMetricsOrDims() throws Exception {
        Query query = testQueries.get(TestQueryName.HAVING_METRICS_OR_DIMS);
        OrFilterExpression orFilter = ((OrFilterExpression)query.getHavingFilter());
        List<FilterPredicate.FilterParameter> params = ((FilterPredicate)orFilter.getRight()).getParameters();
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore,"
                        +"com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating "
                        + "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats "
                        + "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating "
                        + "HAVING (com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating IS NOT NULL "
                        + "OR MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) > "
                        + params.get(0).getPlaceholder() + ")";
        compareQueryLists(expectedQueryStr, engine.showQueries(query));
    }

    /**
     * This test validates that generateCountDistinctClause() is called in the PrestoDialect (same as default/H2).
     */
    @Test
    public void testShowQueriesPagination() {
        String expectedQueryStr1 =
                "SELECT COUNT(DISTINCT(com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating, " +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate)) FROM " +
                        "playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats     ";
        String expectedQueryStr2 =
                "SELECT MIN(com_yahoo_elide_datastores_aggregation_example_PlayerStats.lowScore) AS " +
                        "lowScore,com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS " +
                        "overallRating,PARSEDATETIME(FORMATDATETIME(" +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate, 'yyyy-MM-dd'), " +
                        "'yyyy-MM-dd') AS recordedDate FROM playerStats AS " +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats   " +
                        "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating, " +
                        "PARSEDATETIME(FORMATDATETIME(" +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate, 'yyyy-MM-dd'), " +
                        "'yyyy-MM-dd')  ";
        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);
        compareQueryLists(expectedQueryList, engine.showQueries(testQueries.get(TestQueryName.PAGINATION_TOTAL)));
    }

    /* TODO - Query generation needs to support aliases in ORDER BY to make these pass
    @Test
    public void testShowQuerySortingAscending(){
        String expectedQueryStr =
                "SELECT highScore AS highScoreNoAgg " +
                        "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats   " +
                        "ORDER BY highScore ASC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.showQueries(testQueries.get(TestQueryName.SORT_METRIC_ASC)));
    }

    @Test
    public void testShowQuerySortingDecending(){
        String expectedQueryStr =
                "SELECT highScore AS highScoreNoAgg " +
                        "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats   " +
                        "ORDER BY highScore DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.showQueries(testQueries.get(TestQueryName.SORT_METRIC_DESC)));
    }
    */

    @Test
    public void testShowQuerySortingByDimensionDesc(){
        String expectedQueryStr =
                "SELECT DISTINCT com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS " +
                        "overallRating FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats      " +
                        "ORDER BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.showQueries(testQueries.get(TestQueryName.SORT_DIM_DESC)));
    }

    /* TODO: This test won't work because:
     * 1) dims can only be added in aggregations, which means metrics must be aggregated
     * 2) metrics aggregations are expanded in ORDER BY.
     * Using aliases in ORDER BY will fix this.
     *
    @Test
    public void testShowQuerySortingByMetricAndDimension(){
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) " +
                        "AS highScore,com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS " +
                        "overallRating FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats " +
                        "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating " +
                        "ORDER BY MIN(com_yahoo_elide_datastores_aggregation_example_PlayerStats.lowScore) DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.showQueries(testQueries.get(TestQueryName.SORT_METRIC_AND_DIM_DESC)));
    }
    */


    @Test
    public void testShowQuerySelectFromSubquery() {
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStatsView.highScore) AS " +
                        "highScore FROM (SELECT stats.highScore, stats.player_id, c.name as countryName FROM " +
                        "playerStats AS stats LEFT JOIN countries AS c ON stats.country_id = c.id " +
                        "WHERE stats.overallRating = 'Great') AS " +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStatsView";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.showQueries(testQueries.get(TestQueryName.SUBQUERY)));
    }


    @Test
    public void testShowQueryGroupByNotInSelect() {
        String expectedQueryStr =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore," +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating " +
                        "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats " +
                        "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.showQueries(testQueries.get(TestQueryName.GROUP_BY_DIMENSION_NOT_IN_SELECT)));
    }

    @Test
    public void testShowQueryComplicated() {
        Query query = testQueries.get(TestQueryName.COMPLICATED);
        List<FilterPredicate.FilterParameter> whereParams = ((FilterPredicate)query.getWhereFilter()).getParameters();
        List<FilterPredicate.FilterParameter> havingParams = ((FilterPredicate)query.getHavingFilter()).getParameters();

        String expectedQueryStr1 =
                "SELECT COUNT(DISTINCT(com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating, " +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate)) " +
                        "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats " +
                        "LEFT JOIN countries AS com_yahoo_elide_datastores_aggregation_example_PlayerStats_country " +
                        "ON com_yahoo_elide_datastores_aggregation_example_PlayerStats.country_id = " +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats_country.id " +
                        "WHERE highScore > " + whereParams.get(0).getPlaceholder() + " " +
                        "HAVING LOWER(com_yahoo_elide_datastores_aggregation_example_PlayerStats_country.iso_code) " +
                        "IN (LOWER(" + havingParams.get(0).getPlaceholder() + "))";
        String expectedQueryStr2 =
                "SELECT MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore) AS highScore," +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating AS overallRating," +
                        "PARSEDATETIME(FORMATDATETIME(" +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate, 'yyyy-MM-dd'), " +
                        "'yyyy-MM-dd') AS recordedDate " +
                        "FROM playerStats AS com_yahoo_elide_datastores_aggregation_example_PlayerStats " +
                        "LEFT JOIN countries AS com_yahoo_elide_datastores_aggregation_example_PlayerStats_country " +
                        "ON com_yahoo_elide_datastores_aggregation_example_PlayerStats.country_id = " +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats_country.id " +
                        "WHERE highScore > " + whereParams.get(0).getPlaceholder() + " " +
                        "GROUP BY com_yahoo_elide_datastores_aggregation_example_PlayerStats.overallRating, " +
                        "PARSEDATETIME(FORMATDATETIME(" +
                        "com_yahoo_elide_datastores_aggregation_example_PlayerStats.recordedDate, 'yyyy-MM-dd'), 'yyyy-MM-dd') " +
                        "HAVING LOWER(com_yahoo_elide_datastores_aggregation_example_PlayerStats_country.iso_code) " +
                        "IN (LOWER(" + havingParams.get(0).getPlaceholder() + ")) " +
                        "ORDER BY highScore DESC";
        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.showQueries(query));
    }

}
