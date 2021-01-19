/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import java.lang.annotation.Annotation;

public interface AccessibleObject extends Member {
    String getName();

    default void setAccessible(boolean flag) {
        //NOOP
    }

    default boolean isSynthetic() {
        return false;
    }

    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass);

    Annotation[] getDeclaredAnnotations();

    Annotation[] getAnnotations();
}
