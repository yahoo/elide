/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

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

    private static final Map<Class<?>, ValueType> SCALAR_TYPES = new HashMap<Class<?>, ValueType>() {{
        put(short.class, INTEGER);
        put(Short.class, INTEGER);
        put(int.class, INTEGER);
        put(Integer.class, INTEGER);
        put(long.class, INTEGER);
        put(Long.class, INTEGER);
        put(BigDecimal.class, DECIMAL);
        put(float.class, DECIMAL);
        put(Float.class, DECIMAL);
        put(double.class, DECIMAL);
        put(Double.class, DECIMAL);
        put(boolean.class, BOOLEAN);
        put(Boolean.class, BOOLEAN);
        put(char.class, TEXT);
        put(String.class, TEXT);
    }};

    public static ValueType getScalarType(Class<?> fieldClass) {
        return SCALAR_TYPES.get(fieldClass);
    }
}
