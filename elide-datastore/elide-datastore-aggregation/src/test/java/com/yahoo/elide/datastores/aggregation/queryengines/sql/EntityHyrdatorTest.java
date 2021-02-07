/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.query.ColumnProjection.createSQLAlias;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.timegrains.Day;
import com.yahoo.elide.datastores.aggregation.timegrains.Month;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

public class EntityHyrdatorTest extends SQLUnitTest {

    private static final Answer RESULTSET_NEXT = new Answer() {
        private int count = 0;

        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            if (count == 0) {
                count++;
                return true;
            }
            return false;
        }
    };

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
    }

    @Test
    void testTimeDimensionHydration() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(resultSet.next()).thenAnswer(RESULTSET_NEXT);
        when(resultSet.getObject("highScore")).thenReturn(1234);
        when(resultSet.getObject(createSQLAlias("recordedDate", "byDay"))).thenReturn(new Date(1612390856));
        when(resultSet.getObject(createSQLAlias("recordedDate", "byMonth"))).thenReturn(new Date(1612390856));
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
        PlayerStats stats = (PlayerStats) hydrator.hydrate().iterator().next();

        assertEquals(Month.class, stats.fetch("byMonth", null).getClass());
        assertEquals(Day.class, stats.fetch("byDay", null).getClass());
    }
}
