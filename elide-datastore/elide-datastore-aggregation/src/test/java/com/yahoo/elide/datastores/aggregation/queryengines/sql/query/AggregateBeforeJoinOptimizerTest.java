/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Argument;
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
                getClassType(ClassScanner.getAnnotatedClasses("com.yahoo.elide.datastores.aggregation.example",
                        Include.class)),
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
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`";

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
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`highScore`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` AS `country_id`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate` "
                        + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) > :XXX ) AS `pagination_subquery`\n";

        String expectedQueryStr2 =
                "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) AS `highScore`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate` AS `recordedDate` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`highScore`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` AS `country_id`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country_XXX`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate` "
                        + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "ORDER BY MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`INNER_AGG_XXX`) "
                        + "DESC LIMIT 5 OFFSET 10\n";
        List<String> expectedQueryList = new ArrayList<>();
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
    public void testSortingOnMetricInProjection() {
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
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` "
                        + "ORDER BY MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) DESC\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnMetricInProjection() {
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
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` "
                        + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnDimensionInProjectionNotRequiringJoin() {
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
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` AS `category` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` AS `category` "
                        + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` "
                        + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "OR `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` IN (:XXX))\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnDimensionInProjectionRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression expression = new OrFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(9000)
                ),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                        Operator.IN,
                        Arrays.asList("foo")
                )
        );

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .havingFilter(expression)
                .build();

        compareQueryLists("SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                        + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` "
                        + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "OR `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX))\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnDimensionInProjectionNotRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression having = new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(9000));

        FilterExpression where = new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "category"),
                        Operator.IN,
                        Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("category"))
                .havingFilter(having)
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` AS `category` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` AS `category` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "WHERE `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` "
                + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnDimensionNotInProjectionNotRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where = new AndFilterExpression(
            new FilterPredicate(
                    new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                    Operator.IN,
                    Arrays.asList("foo")),
            new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "category"),
                Operator.IN,
                Arrays.asList("foo")));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "WHERE `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "WHERE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX)\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnDimensionNotInProjectionRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where =
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                        Operator.IN,
                        Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "WHERE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX)\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnDimensionInProjectionRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where =
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                        Operator.IN,
                        Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "WHERE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testSortOnDimensionInProjectionRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(gameRevenueTable)
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .sorting(new SortingImpl(sortMap, GameRevenue.class, dictionary))
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` "
                + "ORDER BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` DESC\n";

                compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testSortOnDimensionInProjectionNotRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("category", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(gameRevenueTable)
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("category"))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .sorting(new SortingImpl(sortMap, GameRevenue.class, dictionary))
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` AS `category` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` AS `category` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` "
                + "ORDER BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` DESC\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }
    @Test
    public void testSortOnTimeDimensionInProjectionRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("sessionDate", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(gameRevenueTable)
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("sessionDate"))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .sorting(new SortingImpl(sortMap, GameRevenue.class, dictionary))
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `sessionDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "GROUP BY PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ORDER BY PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') DESC\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testSortOnTimeDimensionInProjectionNotRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("saleDate", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(gameRevenueTable)
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("saleDate"))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .sorting(new SortingImpl(sortMap, GameRevenue.class, dictionary))
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` AS `saleDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `saleDate` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` "
                + "ORDER BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` DESC\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnTimeDimensionInProjectionNotRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression expression = new OrFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(9000)
                ),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "saleDate"),
                        Operator.IN,
                        Arrays.asList("foo")
                )
        );

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("saleDate"))
                .havingFilter(expression)
                .build();

        compareQueryLists("SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` AS `saleDate` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `saleDate` "
                        + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` "
                        + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "OR `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` IN (:XXX))\n",

                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnTimeDimensionInProjectionNotRequiringJoinWithArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("MONTH")
                .build());

        FilterExpression expression = new OrFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(9000)
                ),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "saleDate", "saleDate", arguments),
                        Operator.IN,
                        Arrays.asList("foo")
                )
        );

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("saleDate", arguments))
                .havingFilter(expression)
                .build();

        compareQueryLists("SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` AS `saleDate` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM'), 'yyyy-MM') AS `saleDate` "
                        + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM'), 'yyyy-MM') ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` "
                        + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "OR `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` IN (:XXX))\n",

                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnTimeDimensionInProjectionRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression expression = new OrFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(9000)
                ),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "sessionDate"),
                        Operator.IN,
                        Arrays.asList("foo")
                )
        );

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("sessionDate"))
                .havingFilter(expression)
                .build();

        compareQueryLists("SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `sessionDate` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                        + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                        + "GROUP BY PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "OR PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX))\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testHavingOnTimeDimensionInProjectionRequiringJoinWithArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("MONTH")
                .build());
        FilterExpression expression = new OrFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(9000)
                ),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "sessionDate", "sessionDate", arguments),
                        Operator.IN,
                        Arrays.asList("foo")
                )
        );

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("sessionDate", arguments))
                .havingFilter(expression)
                .build();

        compareQueryLists("SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM'), 'yyyy-MM') AS `sessionDate` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                        + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                        + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                        + "GROUP BY PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM'), 'yyyy-MM') "
                        + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX "
                        + "OR PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM'), 'yyyy-MM') IN (:XXX))\n",
                engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionInProjectionNotRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression having = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "revenue"),
                Operator.GT,
                Arrays.asList(9000));

        FilterExpression where = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "saleDate"),
                Operator.IN,
                Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("saleDate"))
                .havingFilter(having)
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` AS `saleDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `saleDate` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` "
                + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionInProjectionNotRequiringJoinWithMatchingArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("MONTH")
                .build());

        FilterExpression having = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "revenue"),
                Operator.GT,
                Arrays.asList(9000));

        FilterExpression where = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "saleDate", "saleDate", arguments),
                Operator.IN,
                Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("saleDate", arguments))
                .havingFilter(having)
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` AS `saleDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM'), 'yyyy-MM') AS `saleDate` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM'), 'yyyy-MM') IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM'), 'yyyy-MM') ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` "
                + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionInProjectionNotRequiringJoinWithDefaultMatchingArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("DAY")
                .build());

        FilterExpression having = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "revenue"),
                Operator.GT,
                Arrays.asList(9000));

        FilterExpression where = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "saleDate", "saleDate", arguments),
                Operator.IN,
                Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("saleDate"))
                .havingFilter(having)
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` AS `saleDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `saleDate` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` "
                + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionInProjectionNotRequiringJoinWithMismatchingArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("MONTH")
                .build());

        FilterExpression having = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "revenue"),
                Operator.GT,
                Arrays.asList(9000));

        FilterExpression where = new FilterPredicate(
                new Path(GameRevenue.class, dictionary, "saleDate", "saleDate", arguments),
                Operator.IN,
                Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryIsoCode"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("saleDate"))
                .havingFilter(having)
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` AS `countryIsoCode`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` AS `saleDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `saleDate` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM'), 'yyyy-MM') IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` "
                + "HAVING MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionNotInProjectionNotRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where = new AndFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                        Operator.IN,
                        Arrays.asList("foo")),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "saleDate"),
                        Operator.IN,
                        Arrays.asList("foo")));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "WHERE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX)\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionNotInProjectionNotRequiringJoinWithArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("MONTH")
                .build());

        FilterExpression where = new AndFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                        Operator.IN,
                        Arrays.asList("foo")),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "saleDate", "saleDate", arguments),
                        Operator.IN,
                        Arrays.asList("foo")));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, 'yyyy-MM'), 'yyyy-MM') IN (:XXX) "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "WHERE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX)\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionNotInProjectionRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where =
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "sessionDate"),
                        Operator.IN,
                        Arrays.asList("foo")
                        );

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX)\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionNotInProjectionRequiringJoinWithArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("MONTH")
                .build());
        FilterExpression where =
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "sessionDate", "sessionDate", arguments),
                        Operator.IN,
                        Arrays.asList("foo")
                );

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM'), 'yyyy-MM') IN (:XXX)\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionInProjectionRequiringJoin() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where =
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "sessionDate"),
                        Operator.IN,
                        Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("sessionDate"))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `sessionDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX) "
                + "GROUP BY PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd')\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionInProjectionRequiringJoinWithMismatchingArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        //Grain in filter does not match grain in projected dimension.
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("MONTH")
                .build());

        FilterExpression where =
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "sessionDate", "sessionDate", arguments),
                        Operator.IN,
                        Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("sessionDate"))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `sessionDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM'), 'yyyy-MM') IN (:XXX) "
                + "GROUP BY PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd')\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionInProjectionRequiringJoinWithMatchingArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        //Grain in filter matches grain in projected dimension.
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("MONTH")
                .build());

        FilterExpression where =
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "sessionDate", "sessionDate", arguments),
                        Operator.IN,
                        Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("sessionDate", arguments))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM'), 'yyyy-MM') AS `sessionDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM'), 'yyyy-MM') IN (:XXX) "
                + "GROUP BY PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM'), 'yyyy-MM')\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnTimeDimensionInProjectionRequiringJoinWithDefaultMatchingArguments() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        //Grain in filter matches default grain in projected dimension.
        Set<Argument> arguments = new HashSet<>();
        arguments.add(Argument.builder()
                .name("grain")
                .value("DAY")
                .build());

        FilterExpression where =
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "sessionDate", "sessionDate", arguments),
                        Operator.IN,
                        Arrays.asList("foo"));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("sessionDate"))
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `sessionDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "WHERE PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX) "
                + "GROUP BY PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd')\n";

        compareQueryLists(expected, engine.explain(query));

        testQueryExecution(query);
    }

    @Test
    public void testWhereOnMultiReferenceTimeDimensionNotInProjection() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where = new AndFilterExpression(
            new FilterPredicate(
                    new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                    Operator.IN,
                    Arrays.asList("foo")),
            new FilterPredicate(
                    new Path(GameRevenue.class, dictionary, "lastDate"),
                    Operator.IN,
                    Arrays.asList("foo")));

            Query query = Query.builder()
            .source(gameRevenueTable)
            .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
            .whereFilter(where)
            .build();

            String expected = "SELECT "
                    + "MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue` "
                    + "FROM (SELECT "
                    + "MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                    + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id`,"
                    + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate` AS `saleDate`,"
                    + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                    + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                    + "GROUP BY "
                    + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id`, "
                    + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, "
                    + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                    + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                    + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                    + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                    + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                    + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                    + "WHERE (`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX) "
                    + "AND PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX))";

            compareQueryLists(expected, engine.explain(query));
            testQueryExecution(query);
    }

    @Test
    public void testWhereOnMultiReferenceTimeDimensionInProjection() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where = new AndFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                        Operator.IN,
                        Arrays.asList("foo")),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "lastDate"),
                        Operator.IN,
                        Arrays.asList("foo")));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("lastDate"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `lastDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate` AS `saleDate`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "WHERE (`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX) "
                + "AND PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX)) "
                + "GROUP BY PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd')";

        compareQueryLists(expected, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testSortOnMultiReferenceTimeDimensionInProjection() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lastDate", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("lastDate"))
                .sorting(new SortingImpl(sortMap, GameRevenue.class, dictionary))
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`," +
                "PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `lastDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate` AS `saleDate` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "GROUP BY PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                + "ORDER BY PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') DESC\n";

        compareQueryLists(expected, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testHavingOnMultiReferenceTimeDimensionInProjection() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression having = new OrFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(100)),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "lastDate"),
                        Operator.IN,
                        Arrays.asList("foo")));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .timeDimensionProjection(gameRevenueTable.getTimeDimensionProjection("lastDate"))
                .havingFilter(having)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `lastDate` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id` AS `player_stats_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate` AS `saleDate` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`player_stats_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`saleDate` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`player_stats_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`id` "
                + "GROUP BY PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX "
                + "OR PARSEDATETIME(FORMATDATETIME(CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` > `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_playerStats_XXX`.`recordedDate` ELSE `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`saleDate` END, 'yyyy-MM-dd'), 'yyyy-MM-dd') IN (:XXX))\n";

        compareQueryLists(expected, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testWhereOnMultiReferenceDimensionNotInProjection() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where = new AndFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                        Operator.IN,
                        Arrays.asList("foo")),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryCategory"),
                        Operator.IN,
                        Arrays.asList("US")));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` AS `category` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "WHERE (`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX) "
                + "AND CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END IN (:XXX))\n";

        compareQueryLists(expected, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testWhereOnMultiReferenceDimensionInProjection() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression where = new AndFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryIsoCode"),
                        Operator.IN,
                        Arrays.asList("foo")),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryCategory"),
                        Operator.IN,
                        Arrays.asList("US")));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryCategory"))
                .whereFilter(where)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END AS `countryCategory` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` AS `category` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` ) "
                + "AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "WHERE (`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` IN (:XXX) "
                + "AND CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END IN (:XXX)) "
                + "GROUP BY CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END\n";

        compareQueryLists(expected, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testSortOnMultiReferenceDimensionInProjection() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryCategory", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryCategory"))
                .sorting(new SortingImpl(sortMap, GameRevenue.class, dictionary))
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END AS `countryCategory` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` AS `category` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` ) AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END "
                + "ORDER BY CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END DESC\n";

        compareQueryLists(expected, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testHavingOnMultiReferenceDimensionInProjection() {
        SQLTable gameRevenueTable = (SQLTable) metaDataStore.getTable("gameRevenue", NO_VERSION);

        FilterExpression having = new OrFilterExpression(
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "revenue"),
                        Operator.GT,
                        Arrays.asList(100)),
                new FilterPredicate(
                        new Path(GameRevenue.class, dictionary, "countryCategory"),
                        Operator.IN,
                        Arrays.asList("foo")));

        Query query = Query.builder()
                .source(gameRevenueTable)
                .metricProjection(gameRevenueTable.getMetricProjection("revenue"))
                .dimensionProjection(gameRevenueTable.getDimensionProjection("countryCategory"))
                .havingFilter(having)
                .build();

        String expected = "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) AS `revenue`,"
                + "CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END AS `countryCategory` "
                + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`revenue`) AS `INNER_AGG_XXX`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id` AS `country_id`,"
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` AS `category` "
                + "FROM `gameRevenue` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue` "
                + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`country_id`, "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue`.`category` ) AS "
                + "`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX` "
                + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX` "
                + "ON `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`id` "
                + "GROUP BY CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END "
                + "HAVING (MAX(`com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`INNER_AGG_XXX`) > :XXX OR CASE WHEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX_country_XXX`.`iso_code` = 'US' THEN `com_yahoo_elide_datastores_aggregation_example_GameRevenue_XXX`.`category` ELSE 'UNKNONWN' END IN (:XXX))\n";

        compareQueryLists(expected, engine.explain(query));
        testQueryExecution(query);
    }
}
