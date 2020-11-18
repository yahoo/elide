/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import java.util.TimeZone;

/**
 * Serde class for bidirectional conversion from TimeZone type to String.
 */
@ElideTypeConverter(type = TimeZone.class, name = "TimeZone")
public class TimeZoneSerde implements Serde<String, TimeZone> {

    @Override
    public TimeZone deserialize(String val) {
        TimeZone timezone = TimeZone.getTimeZone(val);
        return timezone;
    }

    @Override
    public String serialize(TimeZone val) {
        return val.getDisplayName(false, TimeZone.SHORT);
    }
}
