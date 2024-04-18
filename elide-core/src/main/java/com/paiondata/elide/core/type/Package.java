/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.type;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Elide package for one or more types.
 */
public interface Package extends Serializable {

    /**
     * Gets the annotations of a specific type.
     * @param annotationClass The annotation to search for.
     * @param <A> The annotation to search for.
     * @return The annotation if found or null.
     */
    <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass);

    /**
     * Returns the name of the package.
     * @return the package name.
     */
    String getName();

    /**
     * Returns the name of the parent package.
     * @return the parent package.
     */
    Package getParentPackage();
}
