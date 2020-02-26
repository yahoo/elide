/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlMax;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
public class MetricReference {
    // PK
    @Setter
    private String id;

    // A metric
    @Setter
    private long highScore;

    // degenerated dimension using sql expression
    @Setter
    private int playerLevel;

    @MetricAggregation(function = SqlMax.class)
    public long getHighScore() {
        return highScore;
    }

    @DimensionFormula("CASE WHEN {{highScore}} = 'Good' THEN 1 ELSE 2 END")
    public int getPlayerLevel() {
        return playerLevel;
    }
}
