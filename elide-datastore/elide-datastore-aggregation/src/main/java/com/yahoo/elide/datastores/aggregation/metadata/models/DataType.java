/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import static com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType.BOOLEAN;
import static com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType.NUMBER;
import static com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType.TEXT;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Data type of a field
 */
@Include(rootLevel = true, type = "dataType")
@Entity
@Data
@AllArgsConstructor
@ToString
public class DataType {
    private static final Map<Class<?>, DataType> SCALAR_TYPES = new HashMap<Class<?>, DataType>() {{
        put(short.class, new DataType("short", NUMBER));
        put(Short.class, new DataType("short", NUMBER));
        put(int.class, new DataType("int", NUMBER));
        put(Integer.class, new DataType("int", NUMBER));
        put(long.class, new DataType("bigint", NUMBER));
        put(Long.class, new DataType("bigint", NUMBER));
        put(BigDecimal.class, new DataType("bigint", NUMBER));
        put(float.class, new DataType("float", NUMBER));
        put(Float.class, new DataType("float", NUMBER));
        put(double.class, new DataType("double", NUMBER));
        put(Double.class, new DataType("double", NUMBER));
        put(boolean.class, new DataType("boolean", BOOLEAN));
        put(Boolean.class, new DataType("boolean", BOOLEAN));
        put(char.class, new DataType("char", TEXT));
        put(String.class, new DataType("string", TEXT));
    }};

    @Id
    private String name;

    private ValueType valueType;

    public static DataType getScalarType(Class<?> valueClass) {
        return SCALAR_TYPES.get(valueClass);
    }
}
