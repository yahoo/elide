/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation.metricformula;

import static com.paiondata.elide.datastores.aggregation.annotation.dimensionformula.DimensionFormulaTest.DUMMY_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

public class MetricFormulaTest {
    @Test
    public void testReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(new DefaultClassScanner(),
                Sets.newHashSet(ClassType.of(MeasureLoop.class)), true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, (unused) -> DUMMY_CONNECTION));
        assertTrue(exception.getMessage().startsWith("Formula reference loop found:"));
    }
}
