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
 * Time Grain class for Year.
 */
public class Year extends Month {

    public static final String FORMAT = "yyyy";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);

    public Year(java.util.Date date) {
        super(date);
    }

    @ElideTypeConverter(type = Year.class, name = "Year")
    static public class YearSerde implements Serde<Object, Year> {
        @Override
        public Year deserialize(Object val) {

            Year date = null;

            try {
                if (val instanceof String) {
                    date = new Year(ISOFormatUtil.formatDateString((String) val, FORMATTER));
                } else {
                    date = new Year(FORMATTER.parse(FORMATTER.format(val)));
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + FORMAT + " or "
                        + ISOFormatUtil.ISO_FORMAT);
            }
            return date;
        }

        @Override
        public String serialize(Year val) {
            return FORMATTER.format(val);
        }
    }
}
