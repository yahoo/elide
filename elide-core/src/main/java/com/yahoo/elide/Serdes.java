/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.core.utils.coerce.converters.EpochToDateConverter;
import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.core.utils.coerce.converters.InstantSerde;
import com.yahoo.elide.core.utils.coerce.converters.OffsetDateTimeSerde;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.core.utils.coerce.converters.TimeZoneSerde;
import com.yahoo.elide.core.utils.coerce.converters.URLSerde;

import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * Contains serializers and deserializers.
 *
 * Use the static factory {@link #builder()} method to prepare an instance.
 *
 */
public class Serdes extends LinkedHashMap<Class, Serde> {

    private static final long serialVersionUID = 1L;

    public static SerdesBuilder builder() {
        return new SerdesBuilder();
    }

    public static class SerdesBuilder extends SerdesBuilderSupport<SerdesBuilder> {
        public Serdes build() {
            Serdes serdes = new Serdes();
            serdes.putAll(this.entries);
            return serdes;
        }

        @Override
        public SerdesBuilder self() {
            return this;
        }
    }

    public static abstract class SerdesBuilderSupport<S> {
        protected Map<Class<?>, Serde<?, ?>> entries = new LinkedHashMap<>();

        public abstract S self();

        public S entries(Consumer<Map<Class<?>, Serde<?, ?>>> entries) {
            entries.accept(this.entries);
            return self();
        }

        public S entry(Class<?> key, Serde<?, ?> value) {
            this.entries.put(key, value);
            return self();
        }

        public S withISO8601Dates(String dateFormat, TimeZone tz) {
            entries.put(Date.class, new ISO8601DateSerde(dateFormat, tz));
            entries.put(java.sql.Date.class, new ISO8601DateSerde(dateFormat, tz, java.sql.Date.class));
            entries.put(java.sql.Time.class, new ISO8601DateSerde(dateFormat, tz, java.sql.Time.class));
            entries.put(java.sql.Timestamp.class, new ISO8601DateSerde(dateFormat, tz, java.sql.Timestamp.class));
            return self();
        }

        public S withEpochDates() {
            entries.put(Date.class, new EpochToDateConverter<>(Date.class));
            entries.put(java.sql.Date.class, new EpochToDateConverter<>(java.sql.Date.class));
            entries.put(java.sql.Time.class, new EpochToDateConverter<>(java.sql.Time.class));
            entries.put(java.sql.Timestamp.class, new EpochToDateConverter<>(java.sql.Timestamp.class));
            return self();
        }

        public S withDefaults() {
            entries.put(Instant.class, new InstantSerde());
            entries.put(OffsetDateTime.class, new OffsetDateTimeSerde());
            entries.put(TimeZone.class, new TimeZoneSerde());
            entries.put(URL.class, new URLSerde());
            return self();
        }
    }
}
