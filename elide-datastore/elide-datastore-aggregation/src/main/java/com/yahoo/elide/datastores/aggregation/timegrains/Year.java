/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.util.Date;

/**
 * Time Grain class for YEAR("yyyy").
 */
public class Year extends Date {

    private static final long serialVersionUID = -4697241489345142589L;

    public Year() {
        super();
    }

    public Year(Date date) {
        super(date.getTime());
    }
}
