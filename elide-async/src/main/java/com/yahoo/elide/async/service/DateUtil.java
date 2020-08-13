/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;

/**
 * Utility class which implements a static method calculateFilterDate.
 */
@Slf4j
public class DateUtil {

    /**
     * Calculated and subtracts the amount based on the calendar unit and amount from current date.
     * @param calendarUnit Enum such as Calendar.DATE or Calendar.MINUTE
     * @param amount Amount of days to be subtracted from current time
     * @return filter date
     */
     protected static Date calculateFilterDate(int calendarUnit, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(calendarUnit, -(amount));
        Date filterDate = cal.getTime();
        log.debug("FilterDateFormatted = {}", filterDate);
        return filterDate;
    }
}
