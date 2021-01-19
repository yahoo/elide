/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public interface Method extends AccessibleObject {
    int getParameterCount();

    Object invoke(Object obj, Object... args) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException;

    Type<?> getReturnType();

    Type<?> getParameterizedReturnType(Type<?> parentType, Optional<Integer> index);

    Class<?>[] getParameterTypes();
}
