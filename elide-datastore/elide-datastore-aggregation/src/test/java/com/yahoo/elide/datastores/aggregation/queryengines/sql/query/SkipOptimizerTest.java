/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.DynamicSQLReferenceTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class SkipOptimizerTest extends SQLUnitTest {

    private static Optimizer optimizer = mock(Optimizer.class);

    @BeforeAll
    public static void init() {
        MetaDataStore metaDataStore = new MetaDataStore(
                getClassType(ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example")),
                false);

        Set<Optimizer> optimizers = new HashSet<>(Arrays.asList(optimizer));
        init(new H2Dialect(), optimizers, me1taDataStore);
    }

    @BeforeEach
    public void beforeEach() {
        reset(optimizer);
    }

    @Test
    public void testSkippingOptimizer() {
        when(optimizer.hint()).thenReturn("Skip");
        Query query = TestQuery.WHERE_AND.getQuery();
        engine.explain(query);

        verify(optimizer, never()).optimize(any(), any());
    }

    @Test
    public void testNegatingOptimizer() {
        when(optimizer.hint()).thenReturn("JoinBeforeAggregate");
        when(optimizer.negateHint()).thenReturn("NoJoinBeforeAggregate");
        Query query = TestQuery.WHERE_AND.getQuery();
        engine.explain(query);

        verify(optimizer, never()).optimize(any(), any());
    }
}
