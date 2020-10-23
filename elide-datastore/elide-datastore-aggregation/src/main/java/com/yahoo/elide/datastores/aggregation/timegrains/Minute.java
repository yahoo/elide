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

/**
 * Time Grain class for Minute.
 */
public class Minute extends Timestamp {

    public static final String FORMAT = "yyyy-MM-dd HH:mm";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Minute(java.util.Date date) {
        super(date.getTime());
    }

    @ElideTypeConverter(type = Minute.class, name = "Minute")
    static public class MinuteSerde implements Serde<Object, Minute> {
        @Override
        public Minute deserialize(Object val) {

            Minute date = null;

            try {
                if (val instanceof String) {
                    date = new Minute(new Timestamp(FORMATTER.parse((String) val).getTime()));
                } else {
                    date = new Minute(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT);
            }

            return date;
        }

        @Override
        public String serialize(Minute val) {
            return FORMATTER.format(val);
        }
    }
}
