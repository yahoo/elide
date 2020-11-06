/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Time Grain class for Hour.
 */
public class Hour extends Minute {

    public static final String FORMAT = "yyyy-MM-dd'T'HH";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Hour(java.util.Date date) {
        super(date);
    }

    @ElideTypeConverter(type = Hour.class, name = "Hour")
    static public class HourSerde implements Serde<Object, Hour> {
        @Override
        public Hour deserialize(Object val) {

            Hour date = null;

            try {
                if (val instanceof String) {
                    date = new Hour(ISOFormatUtil.formatDateString((String) val, FORMATTER));
                } else {
                    date = new Hour(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT);
            }

            return date;
        }

        @Override
        public String serialize(Hour val) {
            return FORMATTER.format(val);
        }
    }
}
