/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

import lombok.Getter;

/**
 * {@link TimeGrain} is a set of concrete {@link TimeGrain} implementations which support "natural" time buckets.
 */
public enum TimeGrain {

    SIMPLEDATE("yyyy-MM-dd"),
    DATETIME("yyyy-MM-dd HH:mm:ss"),
    MONTHYEAR("MMM yyyy"),
    YEARMONTH("yyyy-MM"),
    YEAR("yyyy"),
    WEEKDATEISO("yyyy-MM-dd")
    ;

    @Getter private final String format;

    TimeGrain(final String format) {
        this.format = format;
    }
}
