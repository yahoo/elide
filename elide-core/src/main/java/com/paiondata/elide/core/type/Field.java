/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.type;

import java.util.Optional;

/**
 * A field in an Elide type.
 */
public interface Field extends AccessibleObject {

    /**
     * Get the value of the field.
     * @param obj The object to access.
     * @return The field value.
     * @throws IllegalArgumentException
     * @throws IllegalAccessException if the field cannot be accessed.
     */
    Object get(Object obj) throws IllegalArgumentException, IllegalAccessException;

    /**
     * Get the Elide type associated with the field.
     * @return
     */
    Type<?> getType();

    /**
     * Returns the Elide type of a specific parameter of a parameterized type.
     * @param parentType The parameterized type.
     * @param index Which parameter should be retrieved.
     * @return The parameter type.
     */
    Type<?> getParameterizedType(Type<?> parentType, Optional<Integer> index);

    /**
     * Changes the value of the field.
     * @param obj The object containing the field.
     * @param value The new value.
     * @throws IllegalArgumentException
     * @throws IllegalAccessException If the field cannot be accessed.
     */
    void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;
}
