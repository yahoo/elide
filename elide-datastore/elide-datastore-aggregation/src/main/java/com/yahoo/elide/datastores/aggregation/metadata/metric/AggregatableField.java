/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric;

/**
 * Logical fields that can apply aggregation/metric functions on.
 */
public interface AggregatableField {
    /**
     * Get the field name to reference this field. It can be alias or physical field name.
     *
     * @return field name
     */
    String getName();
}
