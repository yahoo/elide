/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Time Grain class for Quarter.
 */
public class Quarter extends Day {

    public static final String FORMAT = "yyyy-MM";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Quarter(java.util.Date date) {
        super(date);
    }

    @ElideTypeConverter(type = Quarter.class, name = "Quarter")
    static public class QuarterSerde implements Serde<Object, Quarter> {
        private static final SimpleDateFormat MONTH_FORMATTER = new SimpleDateFormat("M");
        private static final Set<String> QUARTER_MONTHS = new HashSet<>(Arrays.asList("1", "4", "7", "10"));

        @Override
        public Quarter deserialize(Object val) {

            Quarter date = null;

            try {
                if (val instanceof String) {
                    date = new Quarter(new Timestamp(FORMATTER.parse((String) val).getTime()));
                } else {
                    date = new Quarter(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT);
            }

            if (!QUARTER_MONTHS.contains(MONTH_FORMATTER.format(date))) {
                throw new IllegalArgumentException("Date string not a quarter month");
            }

            return date;
        }

        @Override
        public String serialize(Quarter val) {
            return FORMATTER.format(val);
        }
    }
}
