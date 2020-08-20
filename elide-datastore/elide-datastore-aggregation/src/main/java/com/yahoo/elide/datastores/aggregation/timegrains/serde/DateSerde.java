/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import com.yahoo.elide.datastores.aggregation.timegrains.Date;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;


import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Serde class for bidirectional conversion from Elide Date type to java.util.Date.
 */
@ElideTypeConverter(type = Date.class, name = "Date")
public class DateSerde implements Serde<String, Date> {

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public Date deserialize(String dateString) {
        System.out.println(dateString);
        Date date;
        try {
            date = new Date(dateFormatter.parse(dateString));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + dateFormatter.toString());
        }
        return date;
    }

    @Override
    public String serialize(Date dateObj) {
        String formattedDate = dateFormatter.format(dateObj);
        return formattedDate;
    }
}
