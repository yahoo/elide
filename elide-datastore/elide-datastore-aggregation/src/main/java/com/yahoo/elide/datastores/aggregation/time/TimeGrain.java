/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.time;

import java.io.Serializable;

/**
 * A {@link TimeGrain} is describes a way of dividing up or organizing time into buckets.
 */
public interface TimeGrain extends Serializable {

    /**
     * Returns the name of this time grain.
     *
     * @return the time grain name
     */
    String getName();
}
