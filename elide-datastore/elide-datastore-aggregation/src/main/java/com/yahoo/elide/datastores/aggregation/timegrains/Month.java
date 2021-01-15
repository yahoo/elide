/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Time Grain class for Month.
 */
public class Month extends Date {

    public static final String FORMAT = "yyyy-MM";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Month(java.util.Date date) {
        super(date.getTime());
    }

    @ElideTypeConverter(type = Month.class, name = "Month")
    static public class MonthSerde implements Serde<Object, Month>, TimeGrainFormatter {
        @Override
        public Month deserialize(Object val) {

            Month date = null;

            try {
                if (val instanceof String) {
                    date = new Month(TimeGrainFormatter.formatDateString(FORMATTER, (String) val));
                } else {
                    date = new Month(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT
                        + " or " + TimeGrainFormatter.ISO_FORMAT);
            }

            return date;
        }

        @Override
        public String serialize(Month val) {
            return FORMATTER.format(val);
        }
    }
}
