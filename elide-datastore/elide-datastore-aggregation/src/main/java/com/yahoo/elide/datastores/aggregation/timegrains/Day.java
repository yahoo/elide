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

/**
 * Time Grain class for Day.
 */
public class Day extends Date implements TimeGrainFormatter {

    public static final String FORMAT = "yyyy-MM-dd";

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
                   date = new Day(TimeGrainFormatter.formatDateString((String) val, "Day"));
                }
                date = new Day(TimeGrainFormatter.DAY_FORMATTER.parse(TimeGrainFormatter.DAY_FORMATTER.format(val)));
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + TimeGrainFormatter.DAY_FORMAT
                        + " or " + TimeGrainFormatter.ISO_FORMAT);
            }
            return date;
        }

        @Override
        public String serialize(Day val) {
            return TimeGrainFormatter.DAY_FORMATTER.format(val);
        }
    }
}
