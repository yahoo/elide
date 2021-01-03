/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

import static com.yahoo.elide.core.utils.TypeHelper.getType;

import com.yahoo.elide.core.type.Type;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Actual value type of a data type.
 */
public enum ValueType {
    TIME,
    INTEGER,
    DECIMAL,
    MONEY,
    TEXT,
    COORDINATE,
    BOOLEAN,
    RELATIONSHIP,
    ID;

    private static final Map<Type<?>, ValueType> SCALAR_TYPES = new HashMap<Type<?>, ValueType>() {{
        put(getType(short.class), INTEGER);
        put(getType(Short.class), INTEGER);
        put(getType(int.class), INTEGER);
        put(getType(Integer.class), INTEGER);
        put(getType(long.class), INTEGER);
        put(getType(Long.class), INTEGER);
        put(getType(BigDecimal.class), DECIMAL);
        put(getType(float.class), DECIMAL);
        put(getType(Float.class), DECIMAL);
        put(getType(double.class), DECIMAL);
        put(getType(Double.class), DECIMAL);
        put(getType(boolean.class), BOOLEAN);
        put(getType(Boolean.class), BOOLEAN);
        put(getType(char.class), TEXT);
        put(getType(String.class), TEXT);
    }};

    public static ValueType getScalarType(Type<?> fieldClass) {
        return SCALAR_TYPES.get(fieldClass);
    }
}
