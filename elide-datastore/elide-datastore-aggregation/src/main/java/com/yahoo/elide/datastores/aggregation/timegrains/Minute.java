/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Time Grain class for Minute.
 */
public class Minute extends Time {

    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT);

    public Minute(Date date) {
        super(date, getSerializer(TimeGrain.MINUTE));
    }

    public Minute(LocalDateTime date) {
        super(date, getSerializer(TimeGrain.MINUTE));
    }

    @ElideTypeConverter(type = Minute.class, name = "Minute")
    static public class MinuteSerde implements Serde<Object, Minute> {
        @Override
        public Minute deserialize(Object val) {
            if (val instanceof Date) {
                return new Minute((Date) val);
            }
            return new Minute(LocalDateTime.parse(val.toString(), FORMATTER));
        }

        @Override
        public String serialize(Minute val) {
            return val.serializer.format(val);
        }
    }
}
