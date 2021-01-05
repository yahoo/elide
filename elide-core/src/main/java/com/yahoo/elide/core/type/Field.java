/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import java.util.Optional;

public interface Field extends AccessibleObject {
    Object get(Object obj) throws IllegalArgumentException, IllegalAccessException;

    Type<?> getType();

    Type<?> getParameterizedType(Type<?> parentType, Optional<Integer> index);

    void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;

    java.lang.reflect.Field getField();
}
