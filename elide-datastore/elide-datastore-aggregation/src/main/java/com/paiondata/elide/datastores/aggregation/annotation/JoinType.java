/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

/**
 * A set of constants that indicates type fo Join.
 */
public enum JoinType {

    LEFT,
    INNER,
    FULL,
    CROSS;
}
