/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import com.yahoo.elide.annotation.Include;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;

@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
public class DimensionReference {
    // PK
    @Setter
    private String id;

    // A metric
    @Setter
    private long highScore;

    // degenerated dimension using sql expression
    @Setter
    private int playerLevel;

    @MetricFormula("{{playerLevel}}")
    public long getHighScore() {
        return highScore;
    }

    @Column(name = "overallRating")
    public int getPlayerLevel() {
        return playerLevel;
    }
}
