/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import java.io.Serializable;

/**
 * Common interface for metric aggregation classes.
 * <p>
 * Implementors should make the concrete implementation immutable.
 */
public interface Aggregation extends Serializable {

    /**
     * Returns the SQL function in {@link String} that represents this {@link Aggregation} operation.
     *
     * @return a string format of SQL function, such as "SUM(%s)"
     */
    String getAggFunctionFormat();
}
