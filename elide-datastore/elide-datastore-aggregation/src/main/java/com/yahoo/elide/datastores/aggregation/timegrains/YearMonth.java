/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.sql.Date;

/**
 * Time Grain class for YEARMONTH("yyyy-MM").
 */
public class YearMonth extends Date {

    private static final long serialVersionUID = -6996481791560356547L;

    public YearMonth(java.util.Date date) {
        super(date.getTime());
    }
}
