/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

public class SimpleDate extends java.util.Date {

    private static final long serialVersionUID = 1L;

    public SimpleDate() {
        super();
    }

    public SimpleDate(java.util.Date date) {
        super(date.getTime());
    }
}
