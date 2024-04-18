/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.timegrains;

import com.paiondata.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.paiondata.elide.core.utils.coerce.converters.Serde;
import com.paiondata.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Time Grain class for Month.
 */
public class Month extends Time {

    public static final String FORMAT = "yyyy-MM";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT)
            .withZone(ZoneOffset.systemDefault());

    public Month(Date date) {
        super(date, true, true, false, false, false, false, getSerializer(TimeGrain.MONTH));
    }

    public Month(LocalDateTime date) {
        super(date, true, true, false, false, false, false, getSerializer(TimeGrain.MONTH));
    }

    @ElideTypeConverter(type = Month.class, name = "Month")
    static public class MonthSerde implements Serde<Object, Month> {
        @Override
        public Month deserialize(Object val) {
            if (val instanceof Date) {
                return new Month((Date) val);
            }
            if (val instanceof OffsetDateTime) {
                OffsetDateTime offsetDateTime = (OffsetDateTime) val;
                return new Month(offsetDateTime.toLocalDateTime());
            }
            YearMonth yearMonth = YearMonth.parse(val.toString(), FORMATTER);
            LocalDateTime localDateTime = LocalDateTime.of(yearMonth.getYear(), yearMonth.getMonth(), 1, 0, 0);
            return new Month(localDateTime);
        }

        @Override
        public String serialize(Month val) {
            return val.serializer.format(val);
        }
    }
}
