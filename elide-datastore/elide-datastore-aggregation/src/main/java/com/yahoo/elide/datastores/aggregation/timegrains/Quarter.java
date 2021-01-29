/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Time Grain class for Quarter.
 */
public class Quarter extends Time {

    public static final String FORMAT = "yyyy-MM";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT)
            .withZone(ZoneId.systemDefault());

    public Quarter(Date date) {
        super(date, getSerializer(TimeGrain.QUARTER));
    }

    public Quarter(LocalDateTime date) {
        super(date, getSerializer(TimeGrain.QUARTER));
    }

    @ElideTypeConverter(type = Quarter.class, name = "Quarter")
    static public class QuarterSerde implements Serde<Object, Quarter> {
        @Override
        public Quarter deserialize(Object val) {
            LocalDateTime date;
            if (val instanceof Date) {
                date = LocalDateTime.ofInstant(((Date) val).toInstant(), ZoneOffset.UTC);
            } else {
                YearMonth yearMonth = YearMonth.parse(val.toString(), FORMATTER);
                date = LocalDateTime.of(yearMonth.getYear(), yearMonth.getMonth(), 1, 0, 0);
            }

            int month = date.getMonthValue();
            if (month != 1 && month != 4 && month != 7 && month != 10) {
                throw new IllegalArgumentException("Date string not a quarter month");
            }

            return new Quarter(date);
        }

        @Override
        public String serialize(Quarter val) {
            return val.serializer.format(val);
        }
    }
}
