/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

/**
 * Type of a logical column based on its physical reference type.
 */
public enum ColumnType {
    FIELD, // reference to a physical field
    REFERENCE, // reference to another logical field
    FORMULA // reference to a logical formula expression
}
