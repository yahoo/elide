/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

/**
 * An aggregation class that aggregates to return max.
 */
public class Max implements Aggregation {

    private static final long serialVersionUID = 3078602564074151821L;

    private static final String AGG_FUNC_FORMAT = "MAX(%s)";

    @Override
    public String getAggFunctionFormat() {
        return AGG_FUNC_FORMAT;
    }
}
