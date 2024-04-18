/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Scans a package for classes by looking at files in the classpath.
 */
public interface ClassScanner {

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param toScan package to scan
     * @param annotation Annotation to search
     * @return The classes
     */
    Set<Class<?>> getAnnotatedClasses(Package toScan, Class<? extends Annotation> annotation);

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName package name to scan.
     * @param annotation Annotation to search
     * @return The classes
     */
    Set<Class<?>> getAnnotatedClasses(String packageName, Class<? extends Annotation> annotation);

    /**
     * Scans all classes accessible from the context class loader which belong to the current class loader.
     * Filters the final output based on expression.
     * @param annotations  One or more annotation to search for
     * @param filter  filter expression to include the final results in the output.
     * @return The classes
     */
    Set<Class<?>> getAnnotatedClasses(List<Class<? extends Annotation>> annotations, FilterExpression filter);

    /**
     * Scans all classes accessible from the context class loader which belong to the current class loader.
     *
     * @param annotations  One or more annotation to search for
     * @return The classes
     */
    Set<Class<?>> getAnnotatedClasses(List<Class<? extends Annotation>> annotations);

    Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> ... annotations);

    /**
     * Returns all classes within a package.
     * @param packageName The root package to search.
     * @return All the classes within a package.
     */
    Set<Class<?>> getAllClasses(String packageName);

    /**
     * Function which will be invoked for deciding to include the class in final results.
     */
    @FunctionalInterface
    interface FilterExpression {
        boolean include(Class clazz);
    }
}
