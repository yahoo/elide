/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.time;

import java.util.Locale;

/**
 * {@link DefaultTimeGrain} is a set of concrete {@link TimeGrain} implementations which support "natural" time buckets.
 */
public enum DefaultTimeGrain implements TimeGrain {

    SECOND,
    MINUTE,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    QUARTER,
    YEAR
    ;

    @Override
    public String getName() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String toString() {
        return getName();
    }
}
