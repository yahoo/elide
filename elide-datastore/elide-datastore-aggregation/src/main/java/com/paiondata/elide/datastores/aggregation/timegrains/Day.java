/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.timegrains;

import com.paiondata.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.paiondata.elide.core.utils.coerce.converters.Serde;
import com.paiondata.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Time Grain class for Day.
 */
public class Day extends Time {

    public static final String FORMAT = "yyyy-MM-dd";

    public Day(Date date) {
        super(date, true, true, true, false, false, false, getSerializer(TimeGrain.DAY));
    }

    public Day(LocalDateTime date) {
        super(date, true, true, true, false, false, false, getSerializer(TimeGrain.DAY));
    }

    @ElideTypeConverter(type = Day.class, name = "Day")
    static public class DaySerde implements Serde<Object, Day> {
        @Override
        public Day deserialize(Object val) {
            if (val instanceof Date) {
                return new Day((Date) val);
            }
            if (val instanceof OffsetDateTime) {
                OffsetDateTime offsetDateTime = (OffsetDateTime) val;
                return new Day(offsetDateTime.toLocalDateTime());
            }
            LocalDate localDate = LocalDate.parse(val.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDateTime localDateTime = localDate.atTime(0, 0);
            return new Day(localDateTime);
        }

        @Override
        public String serialize(Day val) {
            return val.serializer.format(val);
        }
    }
}
