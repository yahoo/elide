/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.cache;

import static com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest.invoke;
import static com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest.toProjection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.request.Sorting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class QueryKeyExtractorTest {

    private static EntityDictionary dictionary;
    private static Table playerStatsTable;

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStats.class);
        playerStatsTable = new Table(PlayerStats.class, dictionary);
    }

    @Test
    public void testMinimalQuery() {
        // check for proper handling of unset Query fields
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(invoke(playerStatsTable.getMetric("highScore")))
                .build();
        assertEquals(
                "com_yahoo_elide_datastores_aggregation_example_PlayerStats;{playerStats.highScore;}{}{};;;;",
                QueryKeyExtractor.extractKey(query));
    }

    @Test
    public void testFullQuery() throws Exception {
        RSQLFilterDialect filterParser = new RSQLFilterDialect(dictionary);
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("playerName", Sorting.SortOrder.asc);
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(invoke(playerStatsTable.getMetric("highScore")))
                .dimensionProjection(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimensionProjection(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.SIMPLEDATE))
                .whereFilter(filterParser.parseFilterExpression("countryNickName=='Uncle Sam'",
                        PlayerStats.class, false))
                .havingFilter(filterParser.parseFilterExpression("highScore > 300",
                        PlayerStats.class, false))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .pagination(new ImmutablePagination(0, 2, false, true))
                .build();
        assertEquals("com_yahoo_elide_datastores_aggregation_example_PlayerStats;" // table name
                        + "{playerStats.highScore;}" // columns
                        + "{playerStats.overallRating;overallRating;{}}" // group by
                        + "{playerStats.recordedDate;recordedDate;{}}" // time dimensions
                        + "{P;{{com.yahoo.elide.datastores.aggregation.example.PlayerStats;java.lang.String;countryNickName;}}IN;9;Uncle Sam;}" // where
                        + "{P;{{com.yahoo.elide.datastores.aggregation.example.PlayerStats;long;highScore;}}GT;3;300;}" // having
                        + "{com.yahoo.elide.datastores.aggregation.example.PlayerStats;{{com.yahoo.elide.datastores.aggregation.example.PlayerStats;java.lang.String;playerName;}}asc;}" // sort
                        + "{0;2;1;}", // pagination
                QueryKeyExtractor.extractKey(query));
    }

    @Test
    public void testColumnsOrdered() {
        assertNotEquals(
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(invoke(playerStatsTable.getMetric("lowScore")))
                        .metricProjection(invoke(playerStatsTable.getMetric("highScore")))
                        .build()),
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(invoke(playerStatsTable.getMetric("highScore")))
                        .metricProjection(invoke(playerStatsTable.getMetric("lowScore")))
                        .build()));
    }

    @Test
    public void testGroupByDimensionsOrdered() {
        assertEquals(
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(invoke(playerStatsTable.getMetric("highScore")))
                        .dimensionProjection(toProjection(playerStatsTable.getDimension("overallRating")))
                        .dimensionProjection(toProjection(playerStatsTable.getDimension("countryNickName")))
                        .build()),
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(invoke(playerStatsTable.getMetric("highScore")))
                        .dimensionProjection(toProjection(playerStatsTable.getDimension("countryNickName")))
                        .dimensionProjection(toProjection(playerStatsTable.getDimension("overallRating")))
                        .build()));
    }

    @Test
    public void testTimeDimensionsOrdered() {
        assertEquals(
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(invoke(playerStatsTable.getMetric("highScore")))
                        .timeDimensionProjection(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.SIMPLEDATE))
                        .timeDimensionProjection(toProjection(playerStatsTable.getTimeDimension("updatedDate"), TimeGrain.SIMPLEDATE))
                        .build()),
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(invoke(playerStatsTable.getMetric("highScore")))
                        .timeDimensionProjection(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.SIMPLEDATE))
                        .timeDimensionProjection(toProjection(playerStatsTable.getTimeDimension("updatedDate"), TimeGrain.SIMPLEDATE))
                        .build()));
    }
}
