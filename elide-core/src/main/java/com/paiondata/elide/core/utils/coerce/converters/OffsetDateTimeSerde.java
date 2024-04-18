/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils.coerce.converters;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Serde class for bidirectional conversion from OffsetDateTime type to String.
 */
public class OffsetDateTimeSerde implements Serde<String, OffsetDateTime> {

    @Override
    public OffsetDateTime deserialize(String val) {
        try {
            return OffsetDateTime.parse(val, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (final DateTimeParseException ex) {
            // Translate parsing exception to something CoerceUtil will handle appropriately
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public String serialize(OffsetDateTime val) {
        return val.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
