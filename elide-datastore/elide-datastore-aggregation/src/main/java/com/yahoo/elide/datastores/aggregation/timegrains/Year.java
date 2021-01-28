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
 * Time Grain class for Year.
 */
public class Year extends Time {

    public static final String FORMAT = "yyyy";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT);

    public Year(Date date) {
        super(date, getSerializer(TimeGrain.YEAR));
    }

    public Year(LocalDateTime date) {
        super(date, getSerializer(TimeGrain.YEAR));
    }

    @ElideTypeConverter(type = Year.class, name = "Year")
    static public class YearSerde implements Serde<Object, Year> {
        @Override
        public Year deserialize(Object val) {
            if (val instanceof Date) {
                return new Year((Date) val);
            }
            return new Year(LocalDateTime.parse(val.toString(), FORMATTER));
        }

        @Override
        public String serialize(Year val) {
            return val.serializer.format(val);
        }
    }
}
