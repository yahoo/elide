/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Time Grain class for ISOWeek.
 */
public class ISOWeek extends Time {

    public static final String FORMAT = "yyyy-MM-dd";

    public ISOWeek(Date date) {
        super(date, getSerializer(TimeGrain.ISOWEEK));
    }

    public ISOWeek(LocalDateTime date) {
        super(date, getSerializer(TimeGrain.ISOWEEK));
    }

    @ElideTypeConverter(type = ISOWeek.class, name = "ISOWeek")
    static public class ISOWeekSerde implements Serde<Object, ISOWeek> {
        @Override
        public ISOWeek deserialize(Object val) {
            LocalDateTime date;
            if (val instanceof Date) {
                date = LocalDateTime.ofInstant(((Date) val).toInstant(), ZoneOffset.UTC);
            } else {
                date = LocalDateTime.parse(val.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
            }

            if (date.getDayOfWeek() != DayOfWeek.MONDAY) {
                throw new IllegalArgumentException("Date string not a Monday");
            }
            return new ISOWeek(date);
        }

        @Override
        public String serialize(ISOWeek val) {
            return val.serializer.format(val);
        }
    }
}
