/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

public enum StandardAggregations {
    SUM,
    COUNT,
    MIN,
    MAX;

    public static StandardAggregations find(String name) {
        for (StandardAggregations current : StandardAggregations.values()) {
            if (current.toString().equals(name)) {
                return current;
            }
        }
        return null;
    }
}
