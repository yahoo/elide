/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class MetricFormulaTest {
    @Test
    public void testReferenceLoop() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Loop.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Table(Loop.class, dictionary));
        assertEquals(
                "Metric formula reference loop found in class loop: highScore->highScore",
                exception.getMessage());
    }

    @Test
    public void testDimensionReference() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(DimensionReference.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Table(DimensionReference.class, dictionary));
        assertEquals(
                "Trying to construct metric field dimensionReference.playerLevel without"
                        + " @MetricAggregation and @MetricFormula.",
                exception.getMessage());
    }
}
