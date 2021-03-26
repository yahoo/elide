/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.enums;

import static com.yahoo.elide.core.type.ClassType.BIGDECIMAL_TYPE;
import static com.yahoo.elide.core.type.ClassType.BOOLEAN_TYPE;
import static com.yahoo.elide.core.type.ClassType.LONG_TYPE;
import static com.yahoo.elide.core.type.ClassType.STRING_TYPE;
import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static com.yahoo.elide.datastores.aggregation.timegrains.Time.TIME_TYPE;

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

    public static Type<?> getType(ValueType valueType) {
        switch (valueType) {
            case TIME:
                return TIME_TYPE;
            case TEXT:
                return STRING_TYPE;
            case INTEGER:
                return LONG_TYPE;
            case MONEY:
                return BIGDECIMAL_TYPE;
            case DECIMAL:
                return BIGDECIMAL_TYPE;
            case COORDINATE:
                return STRING_TYPE;
            case BOOLEAN:
                return BOOLEAN_TYPE;
            default:
                return STRING_TYPE;
        }
    }
}
