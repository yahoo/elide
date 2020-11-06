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
 * Time Grain class for Month.
 */
public class Month extends Day {

    public static final String FORMAT = "yyyy-MM";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Month(java.util.Date date) {
        super(date);
    }

    @ElideTypeConverter(type = Month.class, name = "Month")
    static public class MonthSerde implements Serde<Object, Month> {
        @Override
        public Month deserialize(Object val) {

            Month date = null;

            try {
                if (val instanceof String) {
                    date = new Month(ISOFormatUtil.formatDateString((String) val, FORMATTER));
                } else {
                    date = new Month(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT + " or "
                        + ISOFormatUtil.ISO_FORMAT);
            }

            return date;
        }

        @Override
        public String serialize(Month val) {
            return FORMATTER.format(val);
        }
    }
}
