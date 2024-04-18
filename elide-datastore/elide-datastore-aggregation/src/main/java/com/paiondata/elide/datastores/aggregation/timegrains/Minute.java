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
 * Time Grain class for Minute.
 */
public class Minute extends Time {

    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT)
            .withZone(ZoneOffset.systemDefault());

    public Minute(Date date) {
        super(date, true, true, true, true, true, false, getSerializer(TimeGrain.MINUTE));
    }

    public Minute(LocalDateTime date) {
        super(date, true, true, true, true, true, false, getSerializer(TimeGrain.MINUTE));
    }

    @ElideTypeConverter(type = Minute.class, name = "Minute")
    static public class MinuteSerde implements Serde<Object, Minute> {
        @Override
        public Minute deserialize(Object val) {
            if (val instanceof Date) {
                return new Minute((Date) val);
            }
            if (val instanceof OffsetDateTime) {
                OffsetDateTime offsetDateTime = (OffsetDateTime) val;
                return new Minute(offsetDateTime.toLocalDateTime());
            }
            return new Minute(LocalDateTime.parse(val.toString(), FORMATTER));
        }

        @Override
        public String serialize(Minute val) {
            return val.serializer.format(val);
        }
    }
}
