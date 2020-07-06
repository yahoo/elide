/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Convert an Instant to/from an ISO-8601 string representation.
 *
 * Uses the semantics of {@link java.time.format.DateTimeFormatter#ISO_INSTANT}
 */
@ElideTypeConverter(type = Instant.class, name = "Instant")
public class InstantSerde implements Serde<String, Instant> {
    // NB. ideally we would use ISO_INSTANT here but there is a bug in JDK-8 that
    // means that parsing an ISO offset time doesn't work :-(
    // https://bugs.openjdk.java.net/browse/JDK-8166138
    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public Instant deserialize(final String value) {
        return Instant.from(
            FORMATTER.parse(value)
        );
    }

    @Override
    public String serialize(final Instant value) {
        return FORMATTER.format(value);
    }
}
