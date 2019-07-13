/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

/**
 * An aggregation class that aggregates to return sum.
 */
public class Sum implements Aggregation {

    private static final long serialVersionUID = -6582997654294965367L;

    private static final String AGG_FUNC_FORMAT = "SUM(%s)";

    @Override
    public String getAggFunctionFormat() {
        return AGG_FUNC_FORMAT;
    }
}
