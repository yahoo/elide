/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Time Grain class for Second.
 */
public class Second extends Timestamp {

    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);


    public Second(java.util.Date date) {
        super(date.getTime());
    }

    @Override
    public String toString() {
        return FORMATTER.format(this);
    }

    @ElideTypeConverter(type = Second.class, name = "Second")
    static public class SecondSerde implements Serde<Object, Second> {
        @Override
        public Second deserialize(Object val) {

            Second date = null;

            try {
                if (val instanceof String) {
                    date = new Second(new Timestamp(FORMATTER.parse((String) val).getTime()));
                } else {
                    date = new Second(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT);
            }

            return date;
        }

        @Override
        public String serialize(Second val) {
            return FORMATTER.format(val);
        }
    }
}
