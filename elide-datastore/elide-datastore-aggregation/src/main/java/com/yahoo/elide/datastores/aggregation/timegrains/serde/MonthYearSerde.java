/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.timegrains.MonthYear;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Serde class for bidirectional conversion from Elide MonthYear type to java.util.Date.
 */
@ElideTypeConverter(type = MonthYear.class, name = "MonthYear")
public class MonthYearSerde implements Serde<Object, Date> {

    SimpleDateFormat dateFormatter = new SimpleDateFormat(TimeGrain.MONTHYEAR.getFormat());

    @Override
    public Date deserialize(Object val) {
        Date date = null;

        try {
            if (val instanceof String) {
                date = new Date(dateFormatter.parse((String) val).getTime());
            } else {
                date = new MonthYear(dateFormatter.parse(dateFormatter.format(val)));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + dateFormatter.toPattern());
        }

        return date;
    }

    @Override
    public String serialize(Date val) {
        return dateFormatter.format(val).toString();
    }
}
