/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Time Grain class for Day.
 */
public class Day extends Date {

    public static final String FORMAT = "yyyy-MM-dd";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Day(java.util.Date date) {
        super(date.getTime());
    }

    @ElideTypeConverter(type = Day.class, name = "Day")
    static public class DaySerde implements Serde<Object, Day> {
        @Override
        public Day deserialize(Object val) {

            Day date = null;

            try {
                if (val instanceof String) {
                    date = new Day(new Timestamp(FORMATTER.parse((String) val).getTime()));
                } else {
                    date = new Day(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT);
            }

            return date;
        }

        @Override
        public String serialize(Day val) {
            return FORMATTER.format(val);
        }
    }
}
