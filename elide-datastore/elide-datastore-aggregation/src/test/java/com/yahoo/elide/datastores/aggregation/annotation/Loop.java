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

@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
public class Loop {
    // PK
    @Setter
    private String id;

    // A metric
    @Setter
    private long highScore;

    @MetricFormula("{{highScore}}")
    public long getHighScore() {
        return highScore;
    }
}
