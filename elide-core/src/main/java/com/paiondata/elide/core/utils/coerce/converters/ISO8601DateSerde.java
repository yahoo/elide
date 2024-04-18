/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils.coerce.converters;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Serializes ISO8601 Dates to Strings and vice versa.
 */
public class ISO8601DateSerde implements Serde<Object, Date> {

    protected FastDateFormat df;
    protected Class<? extends Date> targetType;

    public ISO8601DateSerde(SimpleDateFormat df) {
        this(df, Date.class);
    }

    public ISO8601DateSerde(SimpleDateFormat df, Class<? extends Date> targetType) {
        this(df.toPattern(), df.getTimeZone(), targetType);
    }

    public ISO8601DateSerde(String formatString, TimeZone tz) {
        this(formatString, tz, Date.class);
    }

    public ISO8601DateSerde(String formatString, TimeZone tz, Class<? extends Date> targetType) {
        this.df = FastDateFormat.getInstance(formatString, tz);
        this.targetType = targetType;
    }

    public ISO8601DateSerde() {
        this ("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
    }

    @Override
    public Date deserialize(Object val) {
        Date date;

        try {
            if (val instanceof Date) {
                date = (Date) val;
            }
            else {
                date = df.parse(val.toString());
            }
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Date strings must be formatted as " + df.getPattern());
        }

        if (ClassUtils.isAssignable(targetType, java.sql.Date.class)) {
            return new java.sql.Date(date.getTime());
        }
        if (ClassUtils.isAssignable(targetType, java.sql.Timestamp.class)) {
            return new java.sql.Timestamp(date.getTime());
        }
        if (ClassUtils.isAssignable(targetType, java.sql.Time.class)) {
            return new java.sql.Time(date.getTime());
        }
        return date;
    }

    @Override
    public String serialize(Date val) {
        return df.format(val);
    }
}
