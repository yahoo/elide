/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.sql.Date;

/**
 * Time Grain class for WEEKDATE("yyyy-MM-dd").
 */
public class WeekDate extends Date {

    private static final long serialVersionUID = -8590233329032795743L;

    public WeekDate(java.util.Date date) {
        super(date.getTime());
    }
}
