/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.query.ColumnProjection.createSafeAlias;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.timegrains.Day;
import com.yahoo.elide.datastores.aggregation.timegrains.Month;
import example.PlayerStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class EntityHydratorTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
    }

    @Test
    void testEmptyResponse() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);

        Map<String, Argument> monthArguments = new HashMap<>();
        monthArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());

        Map<String, Argument> dayArguments = new HashMap<>();
        dayArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.DAY).build());

        Query query = Query.builder()
                .source(playerStatsTable) .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", "byMonth", monthArguments))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", "byDay", dayArguments))
                .build();

        EntityHydrator hydrator = new EntityHydrator(resultSet, query, dictionary);
        Iterator<Object> iterator = hydrator.iterator();
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, () -> hydrator.iterator().next());
    }

    @Test
    void testTimeDimensionHydration() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject("highScore")).thenReturn(1234);
        when(resultSet.getObject(createSafeAlias("recordedDate", "byDay"))).thenReturn(new Date(1612390856));
        when(resultSet.getObject(createSafeAlias("recordedDate", "byMonth"))).thenReturn(new Date(1612390856));
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(3);

        Map<String, Argument> monthArguments = new HashMap<>();
        monthArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());

        Map<String, Argument> dayArguments = new HashMap<>();
        dayArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.DAY).build());

        Query query = Query.builder()
                .source(playerStatsTable) .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", "byMonth", monthArguments))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", "byDay", dayArguments))
                .build();

        EntityHydrator hydrator = new EntityHydrator(resultSet, query, dictionary);

        Iterator<Object> iterator = hydrator.iterator();
        assertTrue(iterator.hasNext());
        PlayerStats stats = (PlayerStats) iterator.next();

        assertEquals(Month.class, stats.fetch("byMonth", null).getClass());
        assertEquals(Day.class, stats.fetch("byDay", null).getClass());

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, () -> hydrator.iterator().next());
    }

    @Test
    void testNullEnumHydration() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject("overallRating")).thenReturn(null);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);

        Query query = Query.builder()
                .source(playerStatsTable)
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .build();

        EntityHydrator hydrator = new EntityHydrator(resultSet, query, dictionary);

        Iterator<Object> iterator = hydrator.iterator();
        assertTrue(iterator.hasNext());
        PlayerStats stats = (PlayerStats) iterator.next();

        assertNull(stats.getOverallRating());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, () -> hydrator.iterator().next());
    }
}
