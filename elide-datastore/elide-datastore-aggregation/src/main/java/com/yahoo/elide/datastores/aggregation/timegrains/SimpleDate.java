/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.util.Date;

/**
 * Time Grain class for SIMPLEDATE("yyyy-MM-dd").
 */
public class SimpleDate extends Date {

    private static final long serialVersionUID = 6443998660242635314L;

    public SimpleDate() {
        super();
    }

    public SimpleDate(Date date) {
        super(date.getTime());
    }
}
