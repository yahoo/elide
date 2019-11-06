/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.Injector;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Metric;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.security.checks.Check;

import java.util.Map;

/**
 * Dictionary that supports more aggregation data store specific functionality
 */
public class AggregationDictionary extends EntityDictionary {
    public AggregationDictionary(Map<String, Class<? extends Check>> checks) {
        super(checks);
    }

    public AggregationDictionary(Map<String, Class<? extends Check>> checks, Injector injector) {
        super(checks, injector);
    }

    /**
     * Returns whether or not an entity field is a metric field.
     * <p>
     * A field is a metric field iff that field is annotated by at least one of
     * <ol>
     *     <li> {@link MetricAggregation}
     *     <li> {@link MetricComputation}
     * </ol>
     *
     * @param fieldName  The entity field
     *
     * @return {@code true} if the field is a metric field
     */
    public boolean isMetricField(Class<?> cls, String fieldName) {
        return attributeOrRelationAnnotationExists(cls, fieldName, Metric.class);
    }

    public static boolean isAnalyticView(Class<?> cls) {
        return cls.isAnnotationPresent(FromTable.class) || cls.isAnnotationPresent(FromSubquery.class);
    }
}
