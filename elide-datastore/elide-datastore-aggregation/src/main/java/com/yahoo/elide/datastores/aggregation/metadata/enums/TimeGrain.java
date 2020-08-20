/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

//import java.time.Period;

/**
 * {@link TimeGrain} is a set of concrete {@link TimeGrain} implementations which support "natural" time buckets.
 */
public enum TimeGrain {

    DATE("yyyy-MM-dd"),
    DATETIME("yyyy-MM-dd HH:mm:ss")
    ;

    private final String format;

    TimeGrain(final String format) {
        this.format = format;
    }

    /*DATE(Period.ofDays(1)),
    DATETIME(Period.ofWeeks(1)),
    MONTH(Period.ofMonths(1)),
    YEAR(Period.ofYears(1))
    ;*/

    //private final Period period;

    //TimeGrain(final Period period) {
    //    this.period = period;
    //}
}
