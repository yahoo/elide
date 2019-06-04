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

    /**
     * Returns a singleton instance of this {@link Aggregation} function.
     *
     * @return the same function that calculates the sum
     */
    public static Aggregation getInstance() {
        return INSTANCE;
    }

    private static final Aggregation INSTANCE = new Sum();
    private static final String AGG_FUNC_FORMAT = "SUM(%s)";

    @Override
    public String getAggFunctionFormat() {
        return AGG_FUNC_FORMAT;
    }
}
