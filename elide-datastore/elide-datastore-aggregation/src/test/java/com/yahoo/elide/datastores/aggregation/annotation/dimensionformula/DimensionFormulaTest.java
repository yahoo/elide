/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation.dimensionformula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class DimensionFormulaTest {
    @Test
    public void testReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(Sets.newHashSet(Loop.class));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, null, null));
        assertEquals(
                "Formula reference loop found: loop.playerLevel1->loop.playerLevel2->loop.playerLevel1",
                exception.getMessage());
    }

    @Test
    public void testJoinToLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(Sets.newHashSet(JoinToLoop.class));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, null, null));
        assertEquals(
                "Formula reference loop found: joinToLoop.playerLevel->joinToLoop.playerLevel",
                exception.getMessage());
    }

    @Test
    public void testCrossClassReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(
                Sets.newLinkedHashSet(Arrays.asList(LoopCountryA.class, LoopCountryB.class)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, null, null));

        String exception1 = "Formula reference loop found: loopCountryA.inUsa->loopCountryB.inUsa->loopCountryA.inUsa";

        String exception2 = "Formula reference loop found: loopCountryB.inUsa->loopCountryA.inUsa->loopCountryB.inUsa";

        assertTrue(exception1.equals(exception.getMessage()) || exception2.equals(exception.getMessage()));
    }
}
