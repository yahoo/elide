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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Time Grain class for Month.
 */
public class Month extends Time {

    public static final String FORMAT = "yyyy-MM";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT);

    public Month(Date date) {
        super(date, getSerializer(TimeGrain.MONTH));
    }

    public Month(LocalDateTime date) {
        super(date, getSerializer(TimeGrain.MONTH));
    }

    @ElideTypeConverter(type = Month.class, name = "Month")
    static public class MonthSerde implements Serde<Object, Month> {
        @Override
        public Month deserialize(Object val) {
            if (val instanceof Date) {
                return new Month((Date) val);
            }
            return new Month(LocalDateTime.parse(val.toString(), FORMATTER));
        }

        @Override
        public String serialize(Month val) {
            return val.serializer.format(val);
        }
    }
}
