/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Time date type for all analytic model time dimensions.
 */
public class Time extends Date {

    @Getter
    Serializer serializer;

    @FunctionalInterface
    public interface Serializer {
        String format(Time time);
    }

    @ElideTypeConverter(type = Time.class, name = "Time")
    static public class TimeSerde implements Serde<Object, Time> {

        private static final String SECOND_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
        private static final String MINUTE_PATTERN = "yyyy-MM-dd'T'HH:mm";
        private static final String HOUR_PATTERN = "yyyy-MM-dd'T'HH";
        private static final String DATE_PATTERN = "yyyy-MM-dd";
        private static final String MONTH_PATTERN = "yyyy-MM";
        private static final String YEAR_PATTERN = "yyyy";
        private static final String ALL_PATTERNS = String.format("[%s][%s][%s][%s][%s][%s]",
                YEAR_PATTERN,
                MONTH_PATTERN,
                DATE_PATTERN,
                HOUR_PATTERN,
                MINUTE_PATTERN,
                SECOND_PATTERN);

        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ALL_PATTERNS);

        @Override
        public Time deserialize(Object val) {
            try {
                return new Time(LocalDateTime.parse(val.toString(), formatter), (time) -> val.toString());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + ALL_PATTERNS);
            }
        }

        @Override
        public String serialize(Time val) {
            return val.serializer.format(val);
        }
    }

    public Time(LocalDateTime copy, Serializer formatter) {
        super(copy.toEpochSecond(ZoneOffset.UTC));
        this.serializer = formatter;
    }

    public Time(Date copy, Serializer serializer) {
        super(copy.getTime());
        this.serializer = serializer;
    }

    public static Serializer getSerializer(TimeGrain grain) {
        return (time) -> {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(time.toInstant(), ZoneOffset.UTC);
            DateTimeFormatter formatter;
            switch (grain) {
                case SECOND:
                    formatter = Second.FORMATTER;
                    break;
                case MINUTE:
                    formatter = Minute.FORMATTER;
                    break;
                case HOUR:
                    formatter = Hour.FORMATTER;
                    break;
                case MONTH:
                case QUARTER:
                    formatter = Month.FORMATTER;
                    break;
                case YEAR:
                    formatter = Year.FORMATTER;
                    break;
                default:
                    formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            }
            return formatter.format(localDateTime);
        };
    }
}
