/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation.metricformula;

import static com.yahoo.elide.datastores.aggregation.annotation.dimensionformula.DimensionFormulaTest.DUMMY_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

public class MetricFormulaTest {
    @Test
    public void testReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(Sets.newHashSet(ClassType.of(MeasureLoop.class)), true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, DUMMY_CONNECTION));

        String exception1 = "Formula validation failed. Reference Loop detected for: measureLoop.lowScore";
        String exception2 = "Formula validation failed. Reference Loop detected for: measureLoop.highScore";

        assertTrue(exception1.equals(exception.getMessage()) || exception2.equals(exception.getMessage()));
    }
}
