/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.timegrains.YearMonth;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Serde class for bidirectional conversion from Elide YearMonth type to java.util.Date.
 */
@ElideTypeConverter(type = YearMonth.class, name = "YearMonth")
public class YearMonthSerde implements Serde<Object, Date> {

    private static final SimpleDateFormat YEARMONTH_FORMATTER = new SimpleDateFormat(TimeGrain.YEARMONTH.getFormat());

    @Override
    public Date deserialize(Object val) {
        Date date = null;

        try {
            if (val instanceof String) {
                date = new Date(YEARMONTH_FORMATTER.parse((String) val).getTime());
            } else {
                date = new YearMonth(YEARMONTH_FORMATTER.parse(YEARMONTH_FORMATTER.format(val)));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date strings must be formated as " + YEARMONTH_FORMATTER.toPattern());
        }

        return date;
    }

    @Override
    public String serialize(Date val) {
        return YEARMONTH_FORMATTER.format(val).toString();
    }
}
