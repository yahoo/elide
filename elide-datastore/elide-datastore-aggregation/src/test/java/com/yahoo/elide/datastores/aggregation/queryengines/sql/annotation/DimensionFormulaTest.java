/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLAnalyticView;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class DimensionFormulaTest {
    @Test
    public void testReferenceLoop() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Loop.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLAnalyticView(Loop.class, dictionary));
        assertEquals(
                "Dimension formula reference loop found in class loop: "
                        + "[Loop].playerLevel->[Loop].playerLevel",
                exception.getMessage());
    }

    @Test
    public void testCrossClassReferenceLoop() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(LoopCountryA.class);
        dictionary.bindEntity(LoopCountryB.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLTable(LoopCountryB.class, dictionary));
        assertEquals(
                "Dimension formula reference loop found in class loopCountryB: "
                        + "[LoopCountryB].inUsa->"
                        + "[LoopCountryB].countryA/[LoopCountryA].inUsa->"
                        + "[LoopCountryA].countryB/[LoopCountryB].inUsa->"
                        + "[LoopCountryB].countryA/[LoopCountryA].countryB/[LoopCountryB].inUsa->"
                        + "[LoopCountryB].countryA/[LoopCountryA].countryB/[LoopCountryB].inUsa",
                exception.getMessage());
    }

    @Test
    public void testInvalidReference() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(MetricReference.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLAnalyticView(MetricReference.class, dictionary));
        assertEquals(
                "Dimension formula reference to a metric field metricReference: "
                        + "[MetricReference].playerLevel->[MetricReference].highScore",
                exception.getMessage());
    }
}
