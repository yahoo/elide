/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.util.Date;

/**
 * Time Grain class for DATETIME("yyyy-MM-dd HH:mm:ss").
 */
public class DateTime extends Date {

    private static final long serialVersionUID = -4541422985328136461L;

    public DateTime() {
        super();
    }

    public DateTime(Date date) {
        super(date.getTime());
    }
}
