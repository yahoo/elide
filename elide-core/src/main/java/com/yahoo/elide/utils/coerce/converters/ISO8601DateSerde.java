/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Serializes ISO8601 Dates to Strings and vice versa.
 */
public class ISO8601DateSerde implements Serde<String, Date> {

    protected DateFormat df;

    public ISO8601DateSerde() {
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public Date serialize(String val) {
        try {
            return df.parse(val);
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as yyyy-MM-dd'T'HH:mm'Z'");
        }
    }

    @Override
    public String deserialize(Date val) {
        return df.format(val);
    }
}
