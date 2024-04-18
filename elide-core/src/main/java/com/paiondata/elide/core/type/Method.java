/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.type;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * A method belonging to an Elide type.
 */
public interface Method extends AccessibleObject {

    /**
     * Get the number of parameters the method takes.
     * @return The parameter count.
     */
    int getParameterCount();

    /**
     * Invokes the method.
     * @param obj The object containing the method.
     * @param args The parameters
     * @return The return value of the method.
     * @throws IllegalAccessException If the method cannot be accessed.
     * @throws IllegalArgumentException
     * @throws InvocationTargetException If the object doesn't contain this method.
     */
    Object invoke(Object obj, Object... args) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException;

    /**
     * Gets the return type of the method.
     * @return The return type.
     */
    Type<?> getReturnType();

    /**
     * Gets a parameter type if the return type is parameterized.
     * @param parentType The return type.
     * @param index Which parameter type to return.
     * @return The parameter type.
     */
    Type<?> getParameterizedReturnType(Type<?> parentType, Optional<Integer> index);

    /**
     * Returns all the parameter types.
     * @return All the parameter types.
     */
    Class<?>[] getParameterTypes();
}
