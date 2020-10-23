/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Time Grain class for ISOWeek.
 */
public class ISOWeek extends Date {

    public static final String FORMAT = "yyyy-MM-dd";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public ISOWeek(java.util.Date date) {
        super(date.getTime());
    }

    @ElideTypeConverter(type = ISOWeek.class, name = "ISOWeek")
    static public class ISOWeekSerde implements Serde<Object, ISOWeek> {
        private static final SimpleDateFormat WEEKDATE_FORMATTER = new SimpleDateFormat("u");

        @Override
        public ISOWeek deserialize(Object val) {

            ISOWeek date = null;

            try {
                if (val instanceof String) {
                    date = new ISOWeek(new Timestamp(FORMATTER.parse((String) val).getTime()));
                } else {
                    date = new ISOWeek(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT);
            }

            if (!WEEKDATE_FORMATTER.format(date).equals("1")) {
                throw new IllegalArgumentException("Date string not a Monday");
            }

            return date;
        }

        @Override
        public String serialize(ISOWeek val) {
            if (!WEEKDATE_FORMATTER.format(val).equals("1")) {
                throw new IllegalArgumentException("Date string not a Monday");
            }
            return FORMATTER.format(val);
        }
    }
}
