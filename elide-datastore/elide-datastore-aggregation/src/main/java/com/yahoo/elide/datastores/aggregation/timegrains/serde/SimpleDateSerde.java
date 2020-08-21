/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import com.yahoo.elide.datastores.aggregation.timegrains.SimpleDate;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Serde class for bidirectional conversion from Elide SimpleDate type to java.util.Date.
 */
@ElideTypeConverter(type = SimpleDate.class, name = "SimpleDate")
public class SimpleDateSerde implements Serde<Object, SimpleDate> {

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public SimpleDate deserialize(Object dateString) {
        SimpleDate date;
        try {
            date = new SimpleDate(dateFormatter.parse(dateString.toString()));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + dateFormatter.toString());
        }
        return date;
    }

    @Override
    public String serialize(SimpleDate dateObj) {
        String formattedDate = dateFormatter.format(dateObj);
        return formattedDate;
    }
}
