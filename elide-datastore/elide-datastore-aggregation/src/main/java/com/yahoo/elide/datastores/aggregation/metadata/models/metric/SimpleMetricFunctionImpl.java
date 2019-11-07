package com.yahoo.elide.datastores.aggregation.metadata.models.metric;

import java.util.Collections;

public abstract class SimpleMetricFunctionImpl extends MetricFunctionImpl {
    protected SimpleMetricFunctionImpl(String name, String longName, String description) {
        super(name, longName, description, Collections.emptySet());
    }
}
