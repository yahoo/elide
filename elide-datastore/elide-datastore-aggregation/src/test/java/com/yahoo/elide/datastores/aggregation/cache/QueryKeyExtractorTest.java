/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.cache;

import static com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.DEFAULT_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.models.Namespace;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class QueryKeyExtractorTest {

    private static EntityDictionary dictionary;
    private static SQLTable playerStatsTable;

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStats.class);

        Namespace namespace = new Namespace(DEFAULT_NAMESPACE);
        playerStatsTable = new SQLTable(namespace, ClassType.of(PlayerStats.class), dictionary);
    }

    @Test
    public void testMinimalQuery() {
        // check for proper handling of unset Query fields
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .build();
        assertEquals(
                "com_yahoo_elide_datastores_aggregation_example_PlayerStats;{highScore;{}}{}{};;;;",
                QueryKeyExtractor.extractKey(query));
    }

    @Test
    public void testFullQuery() throws Exception {
        RSQLFilterDialect filterParser = new RSQLFilterDialect(dictionary);
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("playerName", Sorting.SortOrder.asc);
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .whereFilter(filterParser.parseFilterExpression("countryNickName=='Uncle Sam'",
                        ClassType.of(PlayerStats.class), false))
                .havingFilter(filterParser.parseFilterExpression("highScore > 300",
                        ClassType.of(PlayerStats.class), false))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .pagination(new ImmutablePagination(0, 2, false, true))
                .build();
        assertEquals("com_yahoo_elide_datastores_aggregation_example_PlayerStats;" // table name
                        + "{highScore;{}}" // columns
                        + "{overallRating;{}}" // group by
                        + "{recordedDate;{}}" // time dimensions
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
                        .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                        .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                        .build()),
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                        .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                        .build()));
    }

    @Test
    public void testGroupByDimensionsOrdered() {
        assertEquals(
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                        .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                        .dimensionProjection(playerStatsTable.getDimensionProjection("countryNickName"))
                        .build()),
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                        .dimensionProjection(playerStatsTable.getDimensionProjection("countryNickName"))
                        .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                        .build()));
    }

    @Test
    public void testTimeDimensionsOrdered() {
        assertEquals(
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                        .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                        .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("updatedDate"))
                        .build()),
                QueryKeyExtractor.extractKey(Query.builder()
                        .source(playerStatsTable)
                        .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                        .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                        .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("updatedDate"))
                        .build()));
    }
}
