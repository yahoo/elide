/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;

/**
 * Where to source values for type-ahead search.
 */
public enum ValueSourceType {
    ENUM,
    TABLE,
    NONE;

    public static ValueSourceType getValueSourceType(Set<String> values, String tableSource) {
        if (CollectionUtils.isNotEmpty(values)) {
            return ValueSourceType.ENUM;
        }
        if (tableSource != null) {
            return ValueSourceType.TABLE;
        }
        return ValueSourceType.NONE;
    }
}
