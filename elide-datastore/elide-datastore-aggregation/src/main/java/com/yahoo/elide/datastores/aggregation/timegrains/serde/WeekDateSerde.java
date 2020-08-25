/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.timegrains.WeekDate;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Serde class for bidirectional conversion from Elide WeekDate type to java.util.Date.
 * WeekDate will be date corresponding to the Monday.
 */
@ElideTypeConverter(type = WeekDate.class, name = "WeekDate")
public class WeekDateSerde implements Serde<Object, Date> {

    SimpleDateFormat dateFormatter = new SimpleDateFormat(TimeGrain.WEEKDATE.getFormat());
    SimpleDateFormat weekdateFormatter = new SimpleDateFormat("u");

    @Override
    public Date deserialize(Object val) {
        Date date = null;

        try {
            if (val instanceof String) {
                date = new Date(dateFormatter.parse((String) val).getTime());
            } else {
                date = new WeekDate(dateFormatter.parse(dateFormatter.format(val)));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + dateFormatter.toPattern());
        }

        if (!weekdateFormatter.format(date).equals("1")) {
            throw new IllegalArgumentException("Date string not a monday.");
        }

        return date;
    }

    @Override
    public String serialize(Date val) {
        if (!weekdateFormatter.format(val).equals("1")) {
            throw new IllegalArgumentException("Date string not a monday.");
        } else {
            return dateFormatter.format(val).toString();
        }
    }
}
