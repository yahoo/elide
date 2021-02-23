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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Time Grain class for Second.
 */
public class Second extends Time {

    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT)
               .withZone(ZoneOffset.systemDefault());

    public Second(Date date) {
        super(date, true, true, true, true, true, true, getSerializer(TimeGrain.SECOND));
    }

    public Second(LocalDateTime date) {
        super(date, true, true, true, true, true, true, getSerializer(TimeGrain.SECOND));
    }

    @ElideTypeConverter(type = Second.class, name = "Second")
    static public class SecondSerde implements Serde<Object, Second> {
        @Override
        public Second deserialize(Object val) {
            if (val instanceof Date) {
                return new Second((Date) val);
            }
            return new Second(LocalDateTime.parse(val.toString(), FORMATTER));
        }

        @Override
        public String serialize(Second val) {
            return val.serializer.format(val);
        }
    }
}
