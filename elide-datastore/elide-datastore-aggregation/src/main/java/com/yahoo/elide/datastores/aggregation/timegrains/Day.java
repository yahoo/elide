/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Time Grain class for Day.
 */
public class Day extends Time {

    public static final String FORMAT = "yyyy-MM-dd";

    public Day(Date date) {
        super(date, getSerializer(TimeGrain.DAY));
    }

    public Day(LocalDateTime date) {
        super(date, getSerializer(TimeGrain.DAY));
    }

    @ElideTypeConverter(type = Day.class, name = "Day")
    static public class DaySerde implements Serde<Object, Day> {
        @Override
        public Day deserialize(Object val) {
            if (val instanceof Date) {
                return new Day((Date) val);
            }
            return new Day(LocalDateTime.parse(val.toString(), DateTimeFormatter.ISO_LOCAL_DATE));
        }

        @Override
        public String serialize(Day val) {
            return val.serializer.format(val);
        }
    }
}
