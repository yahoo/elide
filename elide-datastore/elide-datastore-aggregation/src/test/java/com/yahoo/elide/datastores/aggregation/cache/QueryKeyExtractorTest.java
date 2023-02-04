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
import com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage;
import com.yahoo.elide.datastores.aggregation.dynamic.TableType;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.models.Namespace;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.modelconfig.model.Dimension;
import com.yahoo.elide.modelconfig.model.NamespaceConfig;
import com.yahoo.elide.modelconfig.model.Table;
import com.yahoo.elide.modelconfig.model.Type;
import example.PlayerStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class QueryKeyExtractorTest {

    private static EntityDictionary dictionary;
    private static SQLTable playerStatsTable;

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
        dictionary = EntityDictionary.builder().build();
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
                "example_PlayerStats;{highScore;{}}{}{};;;;",
                QueryKeyExtractor.extractKey(query));
    }

    @Test
    public void testFullQuery() throws Exception {
        RSQLFilterDialect filterParser = RSQLFilterDialect.builder().dictionary(dictionary).build();
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
        assertEquals("example_PlayerStats;" // table name
                        + "{highScore;{}}" // columns
                        + "{overallRating;{}}" // group by
                        + "{recordedDate;{}}" // time dimensions
                        + "{P;{{example.PlayerStats;java.lang.String;countryNickName;}}IN;9;Uncle Sam;}" // where
                        + "{P;{{example.PlayerStats;long;highScore;}}GT;3;300;}" // having
                        + "{example.PlayerStats;{{example.PlayerStats;java.lang.String;playerName;}}asc;}" // sort
                        + "{0;2;1;}", // pagination
                QueryKeyExtractor.extractKey(query));
    }

    @Test
    public void testDuplicateFullQuery() throws Exception {
        // Build 1st Table
        NamespaceConfig testNamespace = NamespaceConfig.builder()
                .name("namespace1")
                .build();
        NamespacePackage testNamespacePackage = new NamespacePackage(testNamespace);

        Table testTable = Table.builder()
                .cardinality("medium")
                .description("A test table")
                .friendlyName("foo")
                .table("table1")
                .name("Table")
                .schema("db1")
                .category("category1")
                .readAccess("Admin")
                .dbConnectionName("dbConn")
                .isFact(true)
                .filterTemplate("a==b")
                .namespace("namespace1")
                .dimension(Dimension.builder()
                        .name("dim1")
                        .definition("{{dim1}}")
                        .type(Type.BOOLEAN)
                        .values(Collections.EMPTY_SET)
                        .tags(Collections.EMPTY_SET)
                        .build())
                .build();
        TableType testType = new TableType(testTable, testNamespacePackage);
        dictionary.bindEntity(testType);

        SQLTable testSqlTable = new SQLTable(new Namespace(testNamespacePackage) , testType, dictionary);

        // Build 2nd Table
        NamespaceConfig anotherTestNamespace = NamespaceConfig.builder()
                .name("namespace2")
                .build();
        NamespacePackage anotherTestNamespacePackage = new NamespacePackage(anotherTestNamespace);

        Table anotherTestTable = Table.builder() // Exactly same as testTable but different namespace
                .cardinality("medium")
                .description("A test table")
                .friendlyName("foo")
                .table("table1")
                .name("Table")
                .schema("db1")
                .category("category1")
                .readAccess("Admin")
                .dbConnectionName("dbConn")
                .isFact(true)
                .filterTemplate("a==b")
                .namespace("namespace2")
                .dimension(Dimension.builder()
                        .name("dim1")
                        .definition("{{dim1}}")
                        .type(Type.BOOLEAN)
                        .values(Collections.EMPTY_SET)
                        .tags(Collections.EMPTY_SET)
                        .build())
                .build();
        TableType anotherTestType = new TableType(anotherTestTable, anotherTestNamespacePackage);
        dictionary.bindEntity(anotherTestType);

        SQLTable anotherTestSqlTable = new SQLTable(new Namespace(anotherTestNamespacePackage) , anotherTestType, dictionary);

        // Build Query and Test
        Query query = Query.builder()
                .source(testSqlTable)
                .dimensionProjection(testSqlTable.getDimensionProjection("dim1"))
                .pagination(new ImmutablePagination(0, 2, false, true))
                .build();

        assertEquals("namespace1_Table;" // table name
                        + "{}" // metrics
                        + "{dim1;{}}" // Group by
                        + "{}" // time dimensions
                        + ";" // where
                        + ";" // having
                        + ";" // sort
                        + "{0;2;1;}", // pagination
                QueryKeyExtractor.extractKey(query));

        Query anotherQuery = Query.builder()
                .source(anotherTestSqlTable)
                .dimensionProjection(anotherTestSqlTable.getDimensionProjection("dim1"))
                .pagination(new ImmutablePagination(0, 2, false, true))
                .build();

        assertEquals("namespace2_Table;" // table name
                        + "{}" // metrics
                        + "{dim1;{}}" // Group by
                        + "{}" // time dimensions
                        + ";" // where
                        + ";" // having
                        + ";" // sort
                        + "{0;2;1;}", // pagination
                QueryKeyExtractor.extractKey(anotherQuery));

        assertNotEquals(QueryKeyExtractor.extractKey(anotherQuery), QueryKeyExtractor.extractKey(query));
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
