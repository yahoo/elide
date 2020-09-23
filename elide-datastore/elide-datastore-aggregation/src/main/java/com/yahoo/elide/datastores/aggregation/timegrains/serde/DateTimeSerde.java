/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.timegrains.DateTime;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Serde class for bidirectional conversion from Elide DateTime type to java.util.Date.
 */
@ElideTypeConverter(type = DateTime.class, name = "DateTime")
public class DateTimeSerde implements Serde<Object, Date> {

    private static final SimpleDateFormat DATETIME_FORMATTER = new SimpleDateFormat(TimeGrain.DATETIME.getFormat());

    @Override
    public Date deserialize(Object val) {

        Date date = null;

        try {
            if (val instanceof String) {
                date = new Date(DATETIME_FORMATTER.parse((String) val).getTime());
            } else {
                date = new DateTime(DATETIME_FORMATTER.parse(DATETIME_FORMATTER.format(val)));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + DATETIME_FORMATTER.toPattern());
        }

        return date;
    }

    @Override
    public String serialize(Date val) {
        return DATETIME_FORMATTER.format(val).toString();
    }
}
