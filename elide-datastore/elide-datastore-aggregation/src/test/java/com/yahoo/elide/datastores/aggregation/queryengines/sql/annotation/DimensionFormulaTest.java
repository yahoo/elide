/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

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
                () -> new SQLQueryEngine(metaDataStore, null));
        assertEquals(
                "Dimension formula reference loop found in class loop: "
                        + "[Loop].playerLevel->[Loop].playerLevel",
                exception.getMessage());
    }

    @Test
    public void testCrossClassReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(
                Sets.newLinkedHashSet(Arrays.asList(LoopCountryA.class, LoopCountryB.class)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, null));

        String exception1 = "Dimension formula reference loop found in class loopCountryB: "
                + "[LoopCountryB].inUsa->"
                + "[LoopCountryB].countryA/[LoopCountryA].inUsa->"
                + "[LoopCountryA].countryB/[LoopCountryB].inUsa->"
                + "[LoopCountryB].countryA/[LoopCountryA].countryB/[LoopCountryB].inUsa->"
                + "[LoopCountryB].countryA/[LoopCountryA].countryB/[LoopCountryB].inUsa";

        String exception2 = "Dimension formula reference loop found in class loopCountryA: "
                + "[LoopCountryA].inUsa->"
                + "[LoopCountryA].countryB/[LoopCountryB].inUsa->"
                + "[LoopCountryB].countryA/[LoopCountryA].inUsa->"
                + "[LoopCountryA].countryB/[LoopCountryB].countryA/[LoopCountryA].inUsa->"
                + "[LoopCountryA].countryB/[LoopCountryB].countryA/[LoopCountryA].inUsa";

        assertTrue(exception1.equals(exception.getMessage()) || exception2.equals(exception.getMessage()));
    }
}
