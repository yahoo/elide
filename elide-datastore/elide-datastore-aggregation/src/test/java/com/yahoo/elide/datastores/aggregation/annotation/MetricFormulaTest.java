/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

public class MetricFormulaTest {
    @Test
    public void testReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(Sets.newHashSet(Loop.class));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, null));
        assertEquals(
                "Formula reference loop found: loop.highScore->loop.highScore",
                exception.getMessage());
    }
}
