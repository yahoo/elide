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

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Serde class for bidirectional conversion from Elide WeekDate type to java.util.Date.
 * WeekDate will be date corresponding to the Monday.
 */
@ElideTypeConverter(type = WeekDate.class, name = "WeekDate")
public class WeekDateSerde implements Serde<Object, Date> {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(TimeGrain.WEEKDATE.getFormat());
    private static final SimpleDateFormat WEEKDATE_FORMATTER = new SimpleDateFormat("u");

    @Override
    public Date deserialize(Object val) {
        Date date = null;

        try {
            if (val instanceof String) {
                date = new Date(DATE_FORMATTER.parse((String) val).getTime());
            } else {
                date = new WeekDate(DATE_FORMATTER.parse(DATE_FORMATTER.format(val)));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + DATE_FORMATTER.toPattern());
        }

        if (!WEEKDATE_FORMATTER.format(date).equals("1")) {
            throw new IllegalArgumentException("Date string not a monday.");
        }

        return date;
    }

    @Override
    public String serialize(Date val) {
        if (!WEEKDATE_FORMATTER.format(val).equals("1")) {
            throw new IllegalArgumentException("Date string not a monday.");
        } else {
            return DATE_FORMATTER.format(val).toString();
        }
    }
}
