package com.yahoo.elide.datastores.aggregation.metadata.models.metric;


import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;

import lombok.Getter;

public class AggregatedField {
    @Getter
    private boolean isMetricField;

    @Getter
    private Metric metric;

    @Getter
    private String alias;

    public AggregatedField(String alias) {
        this.isMetricField = false;
        this.alias = alias;
    }

    public AggregatedField(Metric metric) {
        this.isMetricField = true;
        this.metric = metric;
    }
}
