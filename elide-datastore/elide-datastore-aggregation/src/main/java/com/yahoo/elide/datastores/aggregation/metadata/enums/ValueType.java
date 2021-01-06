/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;

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
        put(getClassType(short.class), INTEGER);
        put(getClassType(Short.class), INTEGER);
        put(getClassType(int.class), INTEGER);
        put(getClassType(Integer.class), INTEGER);
        put(getClassType(long.class), INTEGER);
        put(getClassType(Long.class), INTEGER);
        put(getClassType(BigDecimal.class), DECIMAL);
        put(getClassType(float.class), DECIMAL);
        put(getClassType(Float.class), DECIMAL);
        put(getClassType(double.class), DECIMAL);
        put(getClassType(Double.class), DECIMAL);
        put(getClassType(boolean.class), BOOLEAN);
        put(getClassType(Boolean.class), BOOLEAN);
        put(getClassType(char.class), TEXT);
        put(getClassType(String.class), TEXT);
    }};

    public static ValueType getScalarType(Type<?> fieldClass) {
        return SCALAR_TYPES.get(fieldClass);
    }
}
