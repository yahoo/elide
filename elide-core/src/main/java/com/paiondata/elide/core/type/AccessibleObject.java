/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.type;

import java.lang.annotation.Annotation;

/**
 * Base interface for fields and methods.
 */
public interface AccessibleObject extends Member {

    /**
     * The name of the field or method.
     * @return the name.
     */
    String getName();

    /**
     * Changes whether the field or method is accessible.
     * @param flag true or false.
     */
    default void setAccessible(boolean flag) {
        //NOOP
    }

    /**
     * Determines whether the member is synthetic.
     * @return True or false.
     */
    default boolean isSynthetic() {
        return false;
    }

    /**
     * Checks if an annotation is present.
     * @param annotationClass The annotation to search for.
     * @return True or false.
     */
    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    /**
     * Searches for a specific annotation.
     * @param annotationClass The annotation to search for.
     * @param <T> The annotation type to search for.
     * @return The annotation or null.
     */
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Searches for a specific annotation.
     * @param annotationClass The annotation to search for.
     * @param <T> The annotation type to search for.
     * @return A list of annotations or null.
     */
    <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass);

    /**
     * Returns all annotations ignoring inherited ones.
     * @return A list of annotations or null.
     */
    Annotation[] getDeclaredAnnotations();

    /**
     * Returns all annotations.
     * @return A list of annotations or null.
     */
    Annotation[] getAnnotations();
}
