/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/** Interface for timegrain formatting */
public interface TimeGrainFormatter {

    public String DAY_FORMAT = "yyyy-MM-dd";
    SimpleDateFormat DAY_FORMATTER = new SimpleDateFormat(DAY_FORMAT);

    String HOUR_FORMAT = "yyyy-MM-dd'T'HH";
    SimpleDateFormat HOUR_FORMATTER = new SimpleDateFormat(HOUR_FORMAT);

    String ISOWEEK_FORMAT = "yyyy-MM-dd";
    SimpleDateFormat ISOWEEK_FORMATTER = new SimpleDateFormat(ISOWEEK_FORMAT);

    String MINUTE_FORMAT = "yyyy-MM-dd'T'HH:mm";
    SimpleDateFormat MINUTE_FORMATTER = new SimpleDateFormat(MINUTE_FORMAT);

    String SECOND_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    SimpleDateFormat SECOND_FORMATTER = new SimpleDateFormat(SECOND_FORMAT);

    String QUARTER_FORMAT = "yyyy-MM";
    SimpleDateFormat QUARTER_FORMATTER = new SimpleDateFormat(QUARTER_FORMAT);

    String WEEK_FORMAT = "yyyy-MM-dd";
    SimpleDateFormat WEEK_FORMATTER = new SimpleDateFormat(WEEK_FORMAT);

    String YEAR_FORMAT = "yyyy";
    SimpleDateFormat YEAR_FORMATTER = new SimpleDateFormat(YEAR_FORMAT);

    String MONTH_FORMAT = "yyyy-MM";
    SimpleDateFormat MONTH_FORMATTER = new SimpleDateFormat(MONTH_FORMAT);

    String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    SimpleDateFormat ISO_FORMATTER = new SimpleDateFormat(ISO_FORMAT);

    static Timestamp formatDateString(String val, String type) throws ParseException {
        try {
            switch (type) {
            case "Day":
                return new Timestamp(DAY_FORMATTER.parse((String) val).getTime());

            case "Hour":
                return new Timestamp(HOUR_FORMATTER.parse((String) val).getTime());

            case "ISOWeek":
                return new Timestamp(ISOWEEK_FORMATTER.parse((String) val).getTime());

            case "Minute":
                return new Timestamp(MINUTE_FORMATTER.parse((String) val).getTime());

            case "Month":
                return new Timestamp(MONTH_FORMATTER.parse((String) val).getTime());

            case "Quarter":
                return new Timestamp(QUARTER_FORMATTER.parse((String) val).getTime());

            case "Second":
                return new Timestamp(SECOND_FORMATTER.parse((String) val).getTime());

            case "Week":
                return new Timestamp(WEEK_FORMATTER.parse((String) val).getTime());

            case "Year":
                return new Timestamp(YEAR_FORMATTER.parse((String) val).getTime());

            default:
                return new Timestamp(ISO_FORMATTER.parse((String) val).getTime());
            }
        } catch (ParseException pe) {
            return new Timestamp(ISO_FORMATTER.parse((String) val).getTime());
        }
    }
}
