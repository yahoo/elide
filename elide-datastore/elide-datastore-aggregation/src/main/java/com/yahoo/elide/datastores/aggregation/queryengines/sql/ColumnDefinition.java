/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * Column Definition, used for Handlebars Resolution.
 */
@AllArgsConstructor
@Getter
public class ColumnDefinition {
    private final String expression;
    private final Map<String, Object> defaultColumnArgs;

    @Override
    public String toString() {
        return expression;
    }
}
