/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Serializes ISO8601 Dates to Strings and vice versa.
 */
public class ISO8601DateSerde implements Serde<String, Date> {

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
    public Date deserialize(String val) {
        Date date;

        try {
            date = df.parse(val);
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + df.getPattern());
        }

        if (ClassUtils.isAssignable(targetType, java.sql.Date.class)) {
            return new java.sql.Date(date.getTime());
        } else if (ClassUtils.isAssignable(targetType, java.sql.Timestamp.class)) {
            return new java.sql.Timestamp(date.getTime());
        } else if (ClassUtils.isAssignable(targetType, java.sql.Time.class)) {
            return new java.sql.Time(date.getTime());
        } else {
            return date;
        }
    }

    @Override
    public String serialize(Date val) {
        return df.format(val);
    }
}
