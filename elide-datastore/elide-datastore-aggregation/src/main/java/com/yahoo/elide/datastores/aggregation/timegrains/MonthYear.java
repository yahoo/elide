/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

public class MonthYear extends java.util.Date {

    private static final long serialVersionUID = -6996481791560356547L;

    public MonthYear() {
        super();
    }

    public MonthYear(java.util.Date date) {
        super(date.getTime());
    }
}
