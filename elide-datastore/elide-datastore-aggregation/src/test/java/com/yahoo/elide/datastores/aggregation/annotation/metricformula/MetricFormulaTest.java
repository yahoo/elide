/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation.metricformula;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static com.yahoo.elide.datastores.aggregation.annotation.dimensionformula.DimensionFormulaTest.DUMMY_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

public class MetricFormulaTest {
    @Test
    public void testReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(Sets.newHashSet(getClassType(MeasureLoop.class)), true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, DUMMY_CONNECTION));
        assertTrue(exception.getMessage().startsWith("Formula reference loop found:"));
    }
}
