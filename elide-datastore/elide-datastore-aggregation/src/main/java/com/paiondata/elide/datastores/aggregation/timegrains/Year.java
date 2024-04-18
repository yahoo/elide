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
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Time Grain class for Year.
 */
public class Year extends Time {

    public static final String FORMAT = "yyyy";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT)
            .withZone(ZoneOffset.systemDefault());

    public Year(Date date) {
        super(date, true, false, false, false, false, false, getSerializer(TimeGrain.YEAR));
    }

    public Year(LocalDateTime date) {
        super(date, true, false, false, false, false, false, getSerializer(TimeGrain.YEAR));
    }

    @ElideTypeConverter(type = Year.class, name = "Year")
    static public class YearSerde implements Serde<Object, Year> {
        @Override
        public Year deserialize(Object val) {
            if (val instanceof Date) {
                return new Year((Date) val);
            }
            if (val instanceof OffsetDateTime) {
                OffsetDateTime offsetDateTime = (OffsetDateTime) val;
                return new Year(offsetDateTime.toLocalDateTime());
            }

            java.time.Year year = java.time.Year.parse(val.toString(), FORMATTER);
            LocalDateTime localDateTime = LocalDateTime.of(year.getValue(), Month.of(1), 1, 0, 0);
            return new Year(localDateTime);
        }

        @Override
        public String serialize(Year val) {
            return val.serializer.format(val);
        }
    }
}
