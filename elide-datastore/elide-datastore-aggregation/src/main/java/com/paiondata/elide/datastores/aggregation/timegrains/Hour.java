/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.timegrains;

import com.paiondata.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.paiondata.elide.core.utils.coerce.converters.Serde;
import com.paiondata.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Time Grain class for Hour.
 */
public class Hour extends Time {

    public static final String FORMAT = "yyyy-MM-dd'T'HH";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT)
            .withZone(ZoneOffset.systemDefault());

    public Hour(Date date) {
        super(date, true, true, true, true, false, false, getSerializer(TimeGrain.HOUR));
    }

    public Hour(LocalDateTime date) {
        super(date, true, true, true, true, false, false, getSerializer(TimeGrain.HOUR));
    }

    @ElideTypeConverter(type = Hour.class, name = "Hour")
    static public class HourSerde implements Serde<Object, Hour> {
        @Override
        public Hour deserialize(Object val) {
            if (val instanceof Date) {
                return new Hour((Date) val);
            }
            if (val instanceof OffsetDateTime) {
                OffsetDateTime offsetDateTime = (OffsetDateTime) val;
                return new Hour(offsetDateTime.toLocalDateTime());
            }
            return new Hour(LocalDateTime.parse(val.toString(), FORMATTER));
        }

        @Override
        public String serialize(Hour val) {
            return val.serializer.format(val);
        }
    }
}
