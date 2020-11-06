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
 * Time Grain class for Week.
 */
public class Week extends Day {

    public static final String FORMAT = "yyyy-MM-dd";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Week(java.util.Date date) {
        super(date);
    }

    @ElideTypeConverter(type = Week.class, name = "Week")
    static public class WeekSerde implements Serde<Object, Week> {
        private static final SimpleDateFormat WEEKDATE_FORMATTER = new SimpleDateFormat("u");

        @Override
        public Week deserialize(Object val) {

            Week date = null;

            try {
                if (val instanceof String) {
                    date = new Week(ISOFormatUtil.formatDateString((String) val, FORMATTER));
                } else {
                    date = new Week(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT + " or "
                        + ISOFormatUtil.ISO_FORMAT);
            }

            if (!WEEKDATE_FORMATTER.format(date).equals("7")) {
                throw new IllegalArgumentException("Date string not a Sunday");
            }

            return date;
        }

        @Override
        public String serialize(Week val) {
            return FORMATTER.format(val);
        }
    }
}
