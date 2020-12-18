/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

public interface Field extends AccessibleObject {
    String getName();

    Object get(Object obj) throws IllegalArgumentException, IllegalAccessException;

    Type<?> getType();

    void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;
}
