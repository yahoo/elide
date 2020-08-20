/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

public class DateTime extends java.util.Date {

    private static final long serialVersionUID = 1L;

    public DateTime() {
        super();
    }

    public DateTime(java.util.Date date) {
        super(date.getTime());
    }
}
