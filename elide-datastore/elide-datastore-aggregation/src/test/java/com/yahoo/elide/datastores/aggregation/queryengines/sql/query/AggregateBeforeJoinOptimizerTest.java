/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.example.GameRevenue;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.DynamicSQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AggregateBeforeJoinOptimizerTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        MetaDataStore metaDataStore = new MetaDataStore(
                getClassType(ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example")),
                false);
        Set<Optimizer> optimizers = new HashSet<>(Arrays.asList(new AggregateBeforeJoinOptimizer(metaDataStore)));
        init(new H2Dialect(), optimizers, metaDataStore);
    }

    @Test
    public void testWhereAnd() {
        Query query = TestQuery.WHERE_AND.getQuery();
        String expectedQueryStr =
                "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) AS `highScore`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating` AS `overallRating` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`highScore`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` AS `country_id` "
                        + "FROM `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` IS NOT NULL "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` ) AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`\n";

        compareQueryLists(expectedQueryStr, engine.explain(query));

        testQueryExecution(TestQuery.WHERE_AND.getQuery());
    }

    @Test
    public void testSortAndPaginate() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("highScore", Sorting.SortOrder.desc);
        // WHERE filter
        FilterPredicate predicate = new FilterPredicate(
                new Path(PlayerStats.class, dictionary, "highScore"),
                Operator.GT,
                Arrays.asList(9000));
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .pagination(new ImmutablePagination(10, 5, false, true))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .havingFilter(predicate)
                // force a join to look up countryIsoCode
                .whereFilter(parseFilterExpression("countryIsoCode==USA", playerStatsType, false))
                .build();

        String expectedQueryStr1 =
                "SELECT COUNT(*) FROM "
                + "(SELECT `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`highScore`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` AS `country_id`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) > :XXX ) AS `pagination_subquery`\n";

        String expectedQueryStr2 =
                "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) AS `highScore`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating` AS `overallRating`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`highScore`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` AS `country_id`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "ORDER BY MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) "
                        + "DESC LIMIT 5 OFFSET 10\n";
        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testNoOptimizationNoJoins() {
        Query query = TestQuery.WHERE_DIMS_ONLY.getQuery();

        AggregateBeforeJoinOptimizer optimizer = new AggregateBeforeJoinOptimizer(metaDataStore);

        DynamicSQLReferenceTable lookupTable = new DynamicSQLReferenceTable(engine.getReferenceTable(), query);

        assertFalse(optimizer.canOptimize(query, lookupTable));
    }

    @Test
    public void testNoOptimizationNestedQuery() {
        Query query = Query.builder()
                .source(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                        .build())
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .build();

        AggregateBeforeJoinOptimizer optimizer = new AggregateBeforeJoinOptimizer(metaDataStore);

        DynamicSQLReferenceTable lookupTable = new DynamicSQLReferenceTable(engine.getReferenceTable(), query);

        assertFalse(optimizer.canOptimize(query, lookupTable));
    }

    @Test
    public void testSortingOnMetric() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("revenue", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .sorting(new SortingImpl(sortMap, GameRevenue.class, dictionary))
                .build();

        compareQueryLists("SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`iso_code` AS `countryIsoCode` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country` ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`iso_code` "
                        + "ORDER BY MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) DESC\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnMetric() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterPredicate predicate = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "revenue"),
                Operator.GT,
                Arrays.asList(9000));
        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .havingFilter(predicate)
                .build();

        compareQueryLists("SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`iso_code` AS `countryIsoCode` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country` ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`iso_code` "
                        + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnDimensionNoJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression expression = new OrFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(9000)
                ),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "category"),
                        Operator.IN,
                        Arrays.asList("foo")
                )
        );

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("category"))
                .havingFilter(expression)
                .build();

        compareQueryLists("SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`iso_code` AS `countryIsoCode`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` AS `category` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` AS `category` "
                        + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country`.`iso_code`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` "
                        + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "OR `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` IN (:XXX))\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    //TODO - Tests to add
    /*
    - CONFIRM NESTING ON SIMPLE COLUMN DEFINITIONS - Each column maps to one physical column.
    - * Test Having Clause On Metric In Projection (No Join)
    - * Test Sort Clause On Metric In Projection (No Join)
    - * Test Having Clause On Dimension In Projection (No Join)
    - Test Having Clause On Dimension In Projection (Join)
    - Test Where Clause On Dimension In Projection (No Join)
    - Test Where Clause On Dimension In Projection (Join)
    - Test Where Clause On Dimension Not In Projection (Join)
    - Test Where Clause On Dimension Not In Projection (No Join)
    - Test Sort Clause On Dimension In Projection (No Join)
    - Test Sort Clause On Dimension In Projection (Join)
    - Test Having Clause On TimeDimension In Projection (No Join)
    - Test Having Clause On TimeDimension In Projection (Join)
    - Test Where Clause On TimeDimension In Projection (No Join)
    - Test Where Clause On TimeDimension In Projection (Join)
    - Test Where Clause On TimeDimension Not In Projection (Join)
    - Test Where Clause On TimeDimension Not In Projection (No Join)
    - Test Sort Clause On TimeDimension In Projection (No Join)
    - Test Sort Clause On TimeDimension In Projection (Join)

    - CONFIRM NESTING ON COMPLEX COLUMN DEFINITIONS - Each column maps to one physical column in the
      current table and one join to a physical column in another table:
    - Test Having Clause On TimeDimension In Projection
    - Test Having Clause On Dimension In Projection
    - Test Where Clause On TimeDimension In Projection
    - Test Where Clause On TimeDimension Not In Projection
    - Test Where Clause On Dimension In Projection
    - Test Where Clause On Dimension Not In Projection
    - Test Sort Clause On TimeDimension In Projection
    - Test Sort Clause On Dimension In Projection
     */
}
