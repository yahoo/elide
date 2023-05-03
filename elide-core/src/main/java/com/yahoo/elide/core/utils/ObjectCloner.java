/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils;

import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;

/**
 * Clones an object.
 */
@FunctionalInterface
public interface ObjectCloner {
    <T> T clone(T source, Type<?> cls);

    default <T> T clone(T source) {
        return clone(source, ClassType.of(source.getClass()));
    }
}
