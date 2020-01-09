/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import com.yahoo.elide.utils.coerce.CoerceUtil;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeSerde implements Serde<String, java.time.OffsetDateTime> {
    static {
        CoerceUtil.register(OffsetDateTime.class, new OffsetDateTimeSerde());
    }
    @Override
    public java.time.OffsetDateTime deserialize(String val) {
        return java.time.OffsetDateTime.parse(val, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public String serialize(java.time.OffsetDateTime val) {
        return val.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}

