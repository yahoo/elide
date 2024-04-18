/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils.coerce.converters;

import java.util.TimeZone;

/**
 * Serde class for bidirectional conversion from TimeZone type to String.
 */
public class TimeZoneSerde implements Serde<String, TimeZone> {

    @Override
    public TimeZone deserialize(String val) {
        return TimeZone.getTimeZone(val);
    }

    @Override
    public String serialize(TimeZone val) {
        return val.getDisplayName(false, TimeZone.SHORT);
    }
}
