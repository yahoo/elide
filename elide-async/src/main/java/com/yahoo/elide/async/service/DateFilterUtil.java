/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.utils.coerce.converters.ISO8601DateSerde;

import lombok.extern.slf4j.Slf4j;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
/**
 * Utility class which implements a static method evaluateFormattedFilterDate.
 */
@Slf4j
public class DateFilterUtil {
    /**
     * Evaluates and subtracts the amount based on the calendar unit and amount from current date.
     * @param calendarUnit Enum such as Calendar.DATE or Calendar.MINUTE
     * @param amount Amount of days to be subtracted from current time
     * @return formatted filter date
     */
    protected static String evaluateFormattedFilterDate(Elide elide, int calendarUnit, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(calendarUnit, -(amount));
        Date filterDate = cal.getTime();
        ISO8601DateSerde serde = (ISO8601DateSerde) elide.getElideSettings().getSerdes().get(Date.class);
        String serdePattern = serde.getDf().getPattern();
        Format dateFormat = new SimpleDateFormat(serdePattern);
        String filterDateFormatted = dateFormat.format(filterDate);
        log.debug("FilterDateFormatted = {}", filterDateFormatted);
        return filterDateFormatted;
    }
}
