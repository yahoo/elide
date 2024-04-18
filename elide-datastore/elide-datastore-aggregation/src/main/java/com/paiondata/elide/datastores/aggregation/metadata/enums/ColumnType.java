/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.metadata.enums;

/**
 * Type of a logical column based on its physical reference type.
 */
public enum ColumnType {
    FIELD, // reference to a physical field
    FORMULA // reference to a logical formula expression
}
