/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.models.aggregation;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlSum;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Id;

@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
@FromTable(name = "stats")
public class Stats {

    /**
     * PK.
     */
    @Id
    private String id;

    /**
     * A metric.
     */
    @MetricAggregation(function = SqlSum.class)
    private long measure;

    /**
     * A degenerate dimension.
     */
    private String dimension;
}
