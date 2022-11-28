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
import static com.yahoo.elide.datastores.aggregation.timegrains.Time.TIME_TYPE;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.google.common.collect.ImmutableMap;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Actual value type of a data type.
 */
public enum ValueType {

    TIME("^[-0-9:TZ]+$"),
    INTEGER("^[+-]?\\d+$"),
    DECIMAL("^[+-]?((\\d+(\\.\\d+)?)|(\\.\\d+))$"),
    MONEY("^[+-]?((\\d+(\\.\\d+)?)|(\\.\\d+))$"),
    TEXT("^[a-zA-Z0-9_]+$"),  //Very restricted to prevent SQL Injection
    COORDINATE("^(-?\\d+(\\.\\d+)?)|(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)$"),
    BOOLEAN("^(?i)true|false(?-i)|0|1$"),
    ID("^[a-zA-Z0-9_]+$"),
    UNKNOWN("^$");

    private Pattern pattern;

    ValueType(String regexRestriction) {
        pattern = Pattern.compile(regexRestriction);
    }

    public boolean matches(String input) {
        if (this.equals(UNKNOWN)) {
            return false;
        }

        try {
            CoerceUtil.coerce(input, getType(this));
        } catch (InvalidValueException e) {
            return false;
        }
        return pattern.matcher(input).matches();
    }

    private static final Map<Type<?>, ValueType> SCALAR_TYPES = ImmutableMap.<Type<?>, ValueType>builder()
        .put(ClassType.of(short.class), INTEGER)
        .put(ClassType.of(Short.class), INTEGER)
        .put(ClassType.of(int.class), INTEGER)
        .put(ClassType.of(Integer.class), INTEGER)
        .put(ClassType.of(long.class), INTEGER)
        .put(ClassType.of(Long.class), INTEGER)
        .put(ClassType.of(BigDecimal.class), DECIMAL)
        .put(ClassType.of(float.class), DECIMAL)
        .put(ClassType.of(Float.class), DECIMAL)
        .put(ClassType.of(double.class), DECIMAL)
        .put(ClassType.of(Double.class), DECIMAL)
        .put(ClassType.of(boolean.class), BOOLEAN)
        .put(ClassType.of(Boolean.class), BOOLEAN)
        .put(ClassType.of(char.class), TEXT)
        .put(ClassType.of(String.class), TEXT)
        .build();

    public static ValueType getScalarType(Type<?> fieldClass) {
        return SCALAR_TYPES.getOrDefault(fieldClass, TEXT);
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
