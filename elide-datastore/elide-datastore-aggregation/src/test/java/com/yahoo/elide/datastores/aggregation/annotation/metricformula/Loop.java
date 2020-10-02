/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation.metricformula;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Include
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

    @Setter
    private long lowScore;

    @MetricFormula("{{lowScore}}")
    public long getHighScore() {
        return highScore;
    }

    @MetricFormula("{{highScore}}")
    public long getLowScore() {
        return lowScore;
    }
}
