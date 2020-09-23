/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.timegrains.SimpleDate;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Serde class for bidirectional conversion from Elide SimpleDate type to java.util.Date.
 */
@ElideTypeConverter(type = SimpleDate.class, name = "SimpleDate")
public class SimpleDateSerde implements Serde<Object, Date> {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(TimeGrain.SIMPLEDATE.getFormat());

    @Override
    public Date deserialize(Object val) {
        Date date = null;

        try {
            if (val instanceof String) {
                date = new Date(DATE_FORMATTER.parse((String) val).getTime());
            } else {
                date = new SimpleDate(DATE_FORMATTER.parse(DATE_FORMATTER.format(val)));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + DATE_FORMATTER.toPattern());
        }

        return date;
    }

    @Override
    public String serialize(Date val) {
        return DATE_FORMATTER.format(val).toString();
    }
}
