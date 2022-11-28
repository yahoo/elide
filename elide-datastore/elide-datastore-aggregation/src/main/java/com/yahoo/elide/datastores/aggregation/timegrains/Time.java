/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Time date type for all analytic model time dimensions.
 */
@RequiredArgsConstructor
@Getter
@Setter
public class Time extends Date {

    public static Type<Time> TIME_TYPE = ClassType.of(Time.class);

    protected final Serializer serializer;
    protected final boolean supportsYear;
    protected final boolean supportsMonth;
    protected final boolean supportsDay;
    protected final boolean supportsHour;
    protected final boolean supportsMinute;
    protected final boolean supportsSecond;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Time time = (Time) o;
        return supportsYear == time.supportsYear
                && supportsMonth == time.supportsMonth
                && supportsDay == time.supportsDay
                && supportsHour == time.supportsHour
                && supportsMinute == time.supportsMinute
                && supportsSecond == time.supportsSecond
                && getTime() == time.getTime();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), supportsYear, supportsMonth, supportsDay,
                supportsHour, supportsMinute, supportsSecond, getTime());
    }

    @FunctionalInterface
    public interface Serializer extends Serializable {
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
                SECOND_PATTERN,
                MINUTE_PATTERN,
                HOUR_PATTERN,
                DATE_PATTERN,
                MONTH_PATTERN,
                YEAR_PATTERN);

        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ALL_PATTERNS);

        @Override
        public Time deserialize(Object val) {
            try {
                TemporalAccessor parsed = formatter.parse(val.toString());
                boolean supportsYear = true;
                boolean supportsMonth = parsed.isSupported(ChronoField.MONTH_OF_YEAR);
                boolean supportsDay = parsed.isSupported(ChronoField.DAY_OF_MONTH);
                boolean supportsHour = parsed.isSupported(ChronoField.HOUR_OF_DAY);
                boolean supportsMinute = parsed.isSupported(ChronoField.MINUTE_OF_HOUR);
                boolean supportsSecond = parsed.isSupported(ChronoField.SECOND_OF_MINUTE);

                return new Time(LocalDateTime.of(
                        parsed.get(ChronoField.YEAR),
                        supportsMonth ? parsed.get(ChronoField.MONTH_OF_YEAR) : 1,
                        supportsDay ? parsed.get(ChronoField.DAY_OF_MONTH) : 1,
                        supportsHour ? parsed.get(ChronoField.HOUR_OF_DAY) : 0,
                        supportsMinute ? parsed.get(ChronoField.MINUTE_OF_HOUR) : 0,
                        supportsSecond ? parsed.get(ChronoField.SECOND_OF_MINUTE) : 0),
                        supportsYear,
                        supportsMonth,
                        supportsDay,
                        supportsHour,
                        supportsMinute,
                        supportsSecond,
                        (time) -> val.toString());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("String must be formatted as " + ALL_PATTERNS);
            }
        }

        @Override
        public String serialize(Time val) {
            return val.serializer.format(val);
        }
    }

    public Time(LocalDateTime copy,
                boolean supportsYear,
                boolean supportsMonth,
                boolean supportsDay,
                boolean supportsHour,
                boolean supportsMinute,
                boolean supportsSecond,
                Serializer formatter) {
        super(TimeUnit.SECONDS.toMillis(copy.atZone(ZoneOffset.systemDefault()).toEpochSecond()));
        this.supportsYear = supportsYear;
        this.supportsMonth = supportsMonth;
        this.supportsDay = supportsDay;
        this.supportsHour = supportsHour;
        this.supportsMinute = supportsMinute;
        this.supportsSecond = supportsSecond;
        this.serializer = formatter;
    }

    public Time(Date copy,
                boolean supportsYear,
                boolean supportsMonth,
                boolean supportsDay,
                boolean supportsHour,
                boolean supportsMinute,
                boolean supportsSecond,
                Serializer serializer) {
        super(copy.getTime());
        this.supportsYear = supportsYear;
        this.supportsMonth = supportsMonth;
        this.supportsDay = supportsDay;
        this.supportsHour = supportsHour;
        this.supportsMinute = supportsMinute;
        this.supportsSecond = supportsSecond;
        this.serializer = serializer;
    }

    @Override
    public String toString() {
        return serializer.format(this);
    }

    public static Serializer getSerializer(TimeGrain grain) {
        return (time) -> {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(time.toInstant(), ZoneOffset.systemDefault());
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
