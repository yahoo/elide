/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

import com.yahoo.elide.datastores.aggregation.timegrains.Day;
import com.yahoo.elide.datastores.aggregation.timegrains.Hour;
import com.yahoo.elide.datastores.aggregation.timegrains.ISOWeek;
import com.yahoo.elide.datastores.aggregation.timegrains.Minute;
import com.yahoo.elide.datastores.aggregation.timegrains.Month;
import com.yahoo.elide.datastores.aggregation.timegrains.Quarter;
import com.yahoo.elide.datastores.aggregation.timegrains.Second;
import com.yahoo.elide.datastores.aggregation.timegrains.Week;
import com.yahoo.elide.datastores.aggregation.timegrains.Year;

import lombok.Getter;

/**
 * {@link TimeGrain} is a set of concrete {@link TimeGrain} implementations which support "natural" time buckets.
 */
public enum TimeGrain {

    SECOND(Second.FORMAT),
    MINUTE(Minute.FORMAT),
    HOUR(Hour.FORMAT),
    DAY(Day.FORMAT),
    ISOWEEK(ISOWeek.FORMAT),
    WEEK(Week.FORMAT),
    MONTH(Month.FORMAT),
    QUARTER(Quarter.FORMAT),
    YEAR(Year.FORMAT)
    ;

    @Getter private final String format;

    TimeGrain(final String format) {
        this.format = format;
    }
}
