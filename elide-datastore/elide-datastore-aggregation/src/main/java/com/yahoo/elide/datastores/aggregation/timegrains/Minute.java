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
 * Time Grain class for Minute.
 */
public class Minute extends Second {

    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Minute(java.util.Date date) {
        super(date);
    }

    @ElideTypeConverter(type = Minute.class, name = "Minute")
    static public class MinuteSerde implements Serde<Object, Minute> {
        @Override
        public Minute deserialize(Object val) {

            Minute date = null;

            try {
                if (val instanceof String) {
                    date = new Minute(ISOFormatUtil.formatDateString((String) val, FORMATTER));
                } else {
                    date = new Minute(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT + " or "
                        + ISOFormatUtil.ISO_FORMAT);
            }

            return date;
        }

        @Override
        public String serialize(Minute val) {
            return FORMATTER.format(val);
        }
    }
}
