/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils.coerce.converters;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Convert an Instant to/from an ISO-8601 string representation.
 *
 * Uses the semantics of {@link java.time.format.DateTimeFormatter#ISO_INSTANT}
 */
public class InstantSerde implements Serde<String, Instant> {
    @Override
    public Instant deserialize(final String value) {
        try {
            return Instant.from(
                // NB. ideally we would use ISO_INSTANT here but there is a bug in JDK-8 that
                // means that parsing an ISO offset time doesn't work :-(
                // https://bugs.openjdk.java.net/browse/JDK-8166138
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value)
            );
        } catch (final DateTimeParseException ex) {
            // Translate parsing exception to something CoerceUtil will handle appropriately
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public String serialize(final Instant value) {
        return DateTimeFormatter.ISO_INSTANT.format(value);
    }
}
