/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

import com.paiondata.elide.datastores.aggregation.metadata.enums.TimeGrain;

/**
 * A time grain that a time based dimension can be converted to.
 */
public @interface TimeGrainDefinition {

    /**
     * The unit into which temporal column can be divided.
     *
     * @return One of the supported time grains of a persistent storage column
     */
    TimeGrain grain() default TimeGrain.DAY;

    /**
     * Optional expression used by the QueryEngine to represent the grain natively.
     * The value is query engine specific.
     *
     * @return An expression which defines the grain and is meaningful to the Query Engine.
     */
    String expression() default "{{$$column.expr}}";
}
