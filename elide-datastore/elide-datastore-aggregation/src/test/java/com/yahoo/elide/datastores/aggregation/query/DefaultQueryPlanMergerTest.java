package com.yahoo.elide.datastores.aggregation.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void testCannotMergeMismatchedWhere() {
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
}
