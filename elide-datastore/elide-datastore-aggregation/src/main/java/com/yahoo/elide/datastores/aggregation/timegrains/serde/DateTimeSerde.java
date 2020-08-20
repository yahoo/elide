/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import com.yahoo.elide.datastores.aggregation.timegrains.DateTime;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Serde class for bidirectional conversion from Elide DateTime type to java.util.Date.
 */
@ElideTypeConverter(type = DateTime.class, name = "DateTime")
public class DateTimeSerde implements Serde<String, DateTime> {

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Override
    public DateTime deserialize(String dateString) {
        DateTime dateTime;
        try {
            dateTime = new DateTime(dateFormatter.parse(dateString));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + dateFormatter.toString());
        }
        return dateTime;
    }

    @Override
    public String serialize(DateTime dateObj) {
        String formattedDate = dateFormatter.format(dateObj);
        return formattedDate;
    }
}
