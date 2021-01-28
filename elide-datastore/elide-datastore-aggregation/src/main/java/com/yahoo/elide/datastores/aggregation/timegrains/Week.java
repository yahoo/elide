/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Time Grain class for Week.
 */
public class Week extends Time {

    public Week(Date date) {
        super(date, getSerializer(TimeGrain.WEEK));
    }

    public Week(LocalDateTime date) {
        super(date, getSerializer(TimeGrain.WEEK));
    }

    @ElideTypeConverter(type = Week.class, name = "Week")
    static public class WeekSerde implements Serde<Object, Week> {
        @Override
        public Week deserialize(Object val) {
            LocalDateTime date;
            if (val instanceof Date) {
                date = LocalDateTime.ofInstant(((Date) val).toInstant(), ZoneOffset.UTC);
            } else {
                date = LocalDateTime.parse(val.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
            }

            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                throw new IllegalArgumentException("Date string not a Sunday");
            }
            return new Week(date);
        }

        @Override
        public String serialize(Week val) {
            return val.serializer.format(val);
        }
    }
}
