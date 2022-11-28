/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultQueryPlanMergerTest {

    @Test
    public void testCannotMergeNoNesting() {
        MetaDataStore metaDataStore = mock(MetaDataStore.class);
        DefaultQueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        QueryPlan a = mock(QueryPlan.class);
        QueryPlan b = mock(QueryPlan.class);

        when(a.canNest(any())).thenReturn(false);
        when(b.canNest(any())).thenReturn(true);
        when(a.nestDepth()).thenReturn(1);
        when(b.nestDepth()).thenReturn(2);

        assertFalse(merger.canMerge(a, b));
        assertFalse(merger.canMerge(b, a));
    }

    @Test
    public void testCannotMergeMismatchedUnnestedWhere() {
        MetaDataStore metaDataStore = mock(MetaDataStore.class);
        DefaultQueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        QueryPlan a = mock(QueryPlan.class);
        QueryPlan b = mock(QueryPlan.class);

        when(a.canNest(any())).thenReturn(false);
        when(b.canNest(any())).thenReturn(false);
        when(a.nestDepth()).thenReturn(1);
        when(b.nestDepth()).thenReturn(1);

        FilterExpression expression = mock(FilterExpression.class);

        when(a.getWhereFilter()).thenReturn(expression);
        when(b.getWhereFilter()).thenReturn(null);

        assertFalse(merger.canMerge(a, b));
        assertFalse(merger.canMerge(b, a));
    }

    @Test
    public void testCannotMergeMismatchedNestedWhere() {
        Queryable source = mock(Queryable.class);

        //A root source.
        when(source.getSource()).thenReturn(source);

        MetricProjection m1 = mock(MetricProjection.class);
        MetricProjection m2 = mock(MetricProjection.class);
        when(m1.getName()).thenReturn("m1");
        when(m2.getName()).thenReturn("m2");
        when(m1.canNest(any(), any())).thenReturn(true);
        when(m1.nest(any(), any(), anyBoolean())).thenReturn(Pair.of(m1, Set.of(m1)));
        when(m2.canNest(any(), any())).thenReturn(true);
        when(m2.nest(any(), any(), anyBoolean())).thenReturn(Pair.of(m2, Set.of(m2)));

        FilterExpression filterA = mock(FilterExpression.class);
        FilterExpression filterB = mock(FilterExpression.class);

        QueryPlan a = QueryPlan
                .builder()
                .source(source)
                .whereFilter(filterA)
                .metricProjection(m1)
                .build();

        QueryPlan nested = QueryPlan
                .builder()
                .source(source)
                .whereFilter(filterB)
                .metricProjection(m2)
                .build();

        QueryPlan b = QueryPlan
                .builder()
                .source(nested)
                .metricProjection(m2)
                .build();

        MetaDataStore metaDataStore = mock(MetaDataStore.class);
        DefaultQueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);

        assertFalse(merger.canMerge(a, b));
        assertFalse(merger.canMerge(b, a));
    }

    @Test
    public void testCannotMergeMismatchedDimension() {
        MetaDataStore metaDataStore = mock(MetaDataStore.class);
        DefaultQueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        QueryPlan a = mock(QueryPlan.class);
        QueryPlan b = mock(QueryPlan.class);

        when(a.canNest(any())).thenReturn(false);
        when(b.canNest(any())).thenReturn(false);
        when(a.nestDepth()).thenReturn(1);
        when(b.nestDepth()).thenReturn(1);

        DimensionProjection p1 = mock(DimensionProjection.class);
        Map<String, Argument> args1 = new HashMap<>();
        when(p1.getName()).thenReturn("name");
        when(p1.getArguments()).thenReturn(args1);

        DimensionProjection p2 = mock(DimensionProjection.class);
        Map<String, Argument> args2 = new HashMap<>();
        args2.put("foo", Argument.builder().name("a").value(100).build());
        when(p2.getName()).thenReturn("name");
        when(p2.getArguments()).thenReturn(args2);

        when(a.getDimensionProjections()).thenReturn(List.of(p1));
        when(b.getDimensionProjections()).thenReturn(List.of(p2));
        when(b.getDimensionProjection(eq("name"))).thenReturn(p2);
        when(a.getDimensionProjection(eq("name"))).thenReturn(p1);

        assertFalse(merger.canMerge(a, b));
        assertFalse(merger.canMerge(b, a));
    }

    @Test
    public void testCannotMergeMismatchedTimeDimension() {
        MetaDataStore metaDataStore = mock(MetaDataStore.class);
        DefaultQueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        QueryPlan a = mock(QueryPlan.class);
        QueryPlan b = mock(QueryPlan.class);

        when(a.canNest(any())).thenReturn(false);
        when(b.canNest(any())).thenReturn(false);
        when(a.nestDepth()).thenReturn(1);
        when(b.nestDepth()).thenReturn(1);

        TimeDimensionProjection p1 = mock(TimeDimensionProjection.class);
        Map<String, Argument> args1 = new HashMap<>();
        when(p1.getName()).thenReturn("name");
        when(p1.getArguments()).thenReturn(args1);

        TimeDimensionProjection p2 = mock(TimeDimensionProjection.class);
        Map<String, Argument> args2 = new HashMap<>();
        args2.put("foo", Argument.builder().name("a").value(100).build());
        when(p2.getName()).thenReturn("name");
        when(p2.getArguments()).thenReturn(args2);

        when(a.getTimeDimensionProjections()).thenReturn(List.of(p1));
        when(b.getTimeDimensionProjections()).thenReturn(List.of(p2));
        when(b.getTimeDimensionProjection(eq("name"))).thenReturn(p2);
        when(a.getTimeDimensionProjection(eq("name"))).thenReturn(p1);

        assertFalse(merger.canMerge(a, b));
        assertFalse(merger.canMerge(b, a));
    }

    @Test
    public void testCanMerge() {
        MetaDataStore metaDataStore = mock(MetaDataStore.class);
        DefaultQueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        QueryPlan a = mock(QueryPlan.class);
        QueryPlan b = mock(QueryPlan.class);

        when(a.canNest(any())).thenReturn(false);
        when(b.canNest(any())).thenReturn(false);
        when(a.nestDepth()).thenReturn(1);
        when(b.nestDepth()).thenReturn(1);

        FilterExpression expression = mock(FilterExpression.class);
        when(a.getWhereFilter()).thenReturn(expression);
        when(b.getWhereFilter()).thenReturn(expression);

        TimeDimensionProjection p1 = mock(TimeDimensionProjection.class);
        Map<String, Argument> args1 = new HashMap<>();
        args1.put("foo", Argument.builder().name("a").value(100).build());
        when(p1.getName()).thenReturn("name");
        when(p1.getArguments()).thenReturn(args1);

        TimeDimensionProjection p2 = mock(TimeDimensionProjection.class);
        Map<String, Argument> args2 = new HashMap<>();
        args2.put("foo", Argument.builder().name("a").value(100).build());
        when(p2.getName()).thenReturn("name");
        when(p2.getArguments()).thenReturn(args2);

        when(a.getTimeDimensionProjections()).thenReturn(List.of(p1));
        when(b.getTimeDimensionProjections()).thenReturn(List.of(p2));
        when(b.getTimeDimensionProjection(eq("name"))).thenReturn(p2);
        when(a.getTimeDimensionProjection(eq("name"))).thenReturn(p1);

        assertTrue(merger.canMerge(a, b));
        assertTrue(merger.canMerge(b, a));
    }

    @Test
    public void testSimpleMerge() {
        Queryable source = mock(Queryable.class);

        //A root source.
        when(source.getSource()).thenReturn(source);

        MetricProjection m1 = mock(MetricProjection.class);
        MetricProjection m2 = mock(MetricProjection.class);
        when(m1.getName()).thenReturn("m1");
        when(m2.getName()).thenReturn("m2");

        DimensionProjection d1 = mock(DimensionProjection.class);
        DimensionProjection d2 = mock(DimensionProjection.class);
        when(d1.getName()).thenReturn("d1");
        when(d2.getName()).thenReturn("d2");

        TimeDimensionProjection t1 = mock(TimeDimensionProjection.class);
        TimeDimensionProjection t2 = mock(TimeDimensionProjection.class);
        when(t1.getName()).thenReturn("t1");
        when(t2.getName()).thenReturn("t2");

        QueryPlan a = QueryPlan
                .builder()
                .source(source)
                .metricProjection(m1)
                .dimensionProjection(d1)
                .timeDimensionProjection(t1)
                .build();

        QueryPlan b = QueryPlan
                .builder()
                .source(source)
                .metricProjection(m2)
                .dimensionProjection(d2)
                .timeDimensionProjection(t2)
                .build();

        MetaDataStore metaDataStore = mock(MetaDataStore.class);
        DefaultQueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);

        QueryPlan c = merger.merge(a, b);
        assertEquals(List.of(m2, m1), c.getMetricProjections());
        assertEquals(List.of(d2, d1), c.getDimensionProjections());
        assertEquals(List.of(t2, t1), c.getTimeDimensionProjections());
    }

    @Test
    public void testNestedMerge() {
        Queryable source = mock(Queryable.class);

        //A root source.
        when(source.getSource()).thenReturn(source);

        MetricProjection m1 = mock(MetricProjection.class);
        MetricProjection m2 = mock(MetricProjection.class);
        when(m1.getName()).thenReturn("m1");
        when(m2.getName()).thenReturn("m2");
        when(m1.canNest(any(), any())).thenReturn(true);
        when(m1.nest(any(), any(), anyBoolean())).thenReturn(Pair.of(m1, Set.of(m1)));
        when(m2.canNest(any(), any())).thenReturn(true);
        when(m2.nest(any(), any(), anyBoolean())).thenReturn(Pair.of(m2, Set.of(m2)));

        DimensionProjection d1 = mock(DimensionProjection.class);
        DimensionProjection d2 = mock(DimensionProjection.class);
        when(d1.getName()).thenReturn("d1");
        when(d2.getName()).thenReturn("d2");
        when(d1.canNest(any(), any())).thenReturn(true);
        when(d1.nest(any(), any(), anyBoolean())).thenReturn(Pair.of(d1, Set.of(d1)));
        when(d2.canNest(any(), any())).thenReturn(true);
        when(d2.nest(any(), any(), anyBoolean())).thenReturn(Pair.of(d2, Set.of(d2)));

        TimeDimensionProjection t1 = mock(TimeDimensionProjection.class);
        TimeDimensionProjection t2 = mock(TimeDimensionProjection.class);
        when(t1.getName()).thenReturn("t1");
        when(t2.getName()).thenReturn("t2");
        when(t1.canNest(any(), any())).thenReturn(true);
        when(t1.nest(any(), any(), anyBoolean())).thenReturn(Pair.of(t1, Set.of(t1)));
        when(t2.canNest(any(), any())).thenReturn(true);
        when(t2.nest(any(), any(), anyBoolean())).thenReturn(Pair.of(t2, Set.of(t2)));

        FilterExpression filterExpression = mock(FilterExpression.class);

        QueryPlan a = QueryPlan
                .builder()
                .source(source)
                .whereFilter(filterExpression)
                .metricProjection(m1)
                .dimensionProjection(d1)
                .timeDimensionProjection(t1)
                .build();

        QueryPlan nested = QueryPlan
                .builder()
                .source(source)
                .metricProjection(m2)
                .dimensionProjection(d2)
                .timeDimensionProjection(t2)
                .build();

        QueryPlan b = QueryPlan
                .builder()
                .source(nested)
                .metricProjection(m2)
                .dimensionProjection(d2)
                .timeDimensionProjection(t2)
                .build();

        MetaDataStore metaDataStore = mock(MetaDataStore.class);
        DefaultQueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);

        QueryPlan c = merger.merge(a, b);
        assertNull(c.getWhereFilter());
        assertEquals(List.of(m2, m1), c.getMetricProjections());
        assertEquals(List.of(d2, d1), c.getDimensionProjections());
        assertEquals(List.of(t2, t1), c.getTimeDimensionProjections());

        QueryPlan d = (QueryPlan) c.getSource();
        assertEquals(List.of(m2, m1), d.getMetricProjections());
        assertEquals(List.of(d2, d1), d.getDimensionProjections());
        assertEquals(List.of(t2, t1), d.getTimeDimensionProjections());
        assertEquals(filterExpression, d.getWhereFilter());
    }

    @Test
    public void testMultipleMergeSuccess() {
        QueryPlan a = mock(QueryPlan.class);
        QueryPlan b = mock(QueryPlan.class);
        QueryPlan c = mock(QueryPlan.class);

        QueryPlanMerger merger = new QueryPlanMerger() {
            @Override
            public boolean canMerge(QueryPlan a, QueryPlan b) {
                return true;
            }

            @Override
            public QueryPlan merge(QueryPlan a, QueryPlan b) {
                return mock(QueryPlan.class);
            }
        };

        List<QueryPlan> results = merger.merge(List.of(a, b, c));

        assertEquals(1, results.size());
    }

    @Test
    public void testMultipleMergeFailure() {
        QueryPlan a = mock(QueryPlan.class);
        QueryPlan b = mock(QueryPlan.class);
        QueryPlan c = mock(QueryPlan.class);

        QueryPlanMerger merger = new QueryPlanMerger() {
            @Override
            public boolean canMerge(QueryPlan a, QueryPlan b) {
                return false;
            }

            @Override
            public QueryPlan merge(QueryPlan a, QueryPlan b) {
                return mock(QueryPlan.class);
            }
        };

        List<QueryPlan> results = merger.merge(List.of(a, b, c));

        assertEquals(3, results.size());
        assertEquals(List.of(a, b, c), results);
    }
}
