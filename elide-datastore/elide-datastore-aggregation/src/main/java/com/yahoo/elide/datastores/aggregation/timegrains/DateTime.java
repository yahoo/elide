/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.sql.Timestamp;

/**
 * Time Grain class for DATETIME("yyyy-MM-dd HH:mm:ss").
 */
public class DateTime extends Timestamp {

    private static final long serialVersionUID = -4541422985328136461L;

    public DateTime(java.util.Date date) {
        super(date.getTime());
    }
}
