/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans a package for classes by looking at files in the classpath.
 */
public class ClassScanner {

    /**
     * Class Scanning is terribly costly for service boot, so we do all the scanning up front once to
     * save on startup costs.  All Annotations Elide scans for must be listed here:
     */
    private static final String [] CACHE_ANNOTATIONS  = {
            //Elide Core Annotations
            "com.yahoo.elide.annotation.Include",
            "com.yahoo.elide.annotation.SecurityCheck",
            "com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter",

            //Aggregation Store Annotations
            "com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable",
            "com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery",
            "org.hibernate.annotations.Subselect",
            "javax.persistence.Entity",
            "javax.persistence.Table"
    };

    private static final Map<String, Set<Class<?>>> STARTUP_CACHE = new HashMap<>();

    static {
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().enableAnnotationInfo().scan()) {
            for (String annotationName : CACHE_ANNOTATIONS) {
                STARTUP_CACHE.put(annotationName, scanResult.getClassesWithAnnotation(annotationName)
                        .stream()
                        .map(ClassInfo::loadClass)
                        .collect(Collectors.toSet()));

            }
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param toScan package to scan
     * @param annotation Annotation to search
     * @return The classes
     */
    static public Set<Class<?>> getAnnotatedClasses(Package toScan, Class<? extends Annotation> annotation) {
        return getAnnotatedClasses(toScan.getName(), annotation);
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName package name to scan.
     * @param annotation Annotation to search
     * @return The classes
     */
    static public Set<Class<?>> getAnnotatedClasses(String packageName, Class<? extends Annotation> annotation) {
        return STARTUP_CACHE.get(annotation.getCanonicalName()).stream()
                .filter(clazz ->
                        clazz.getPackage().getName().equals(packageName)
                                || clazz.getPackage().getName().startsWith(packageName + "."))
                .collect(Collectors.toSet());
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the current class loader.
     * Filters the final output based on expression.
     * @param annotations  One or more annotation to search for
     * @param filter  filter expression to include the final results in the output.
     * @return The classes
     */
    static public Set<Class<?>> getAnnotatedClasses(List<Class<? extends Annotation>> annotations,
            FilterExpression filter) {
        Set<Class<?>> result = new HashSet<>();

        for (Class<? extends Annotation> annotation : annotations) {
            result.addAll(STARTUP_CACHE.get(annotation.getCanonicalName()).stream()
                    .filter(filter::include)
                    .collect(Collectors.toSet()));
        }

        return result;
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the current class loader.
     *
     * @param annotations  One or more annotation to search for
     * @return The classes
     */
    static public Set<Class<?>> getAnnotatedClasses(List<Class<? extends Annotation>> annotations) {
        return getAnnotatedClasses(annotations, clazz -> true);
    }

    @SafeVarargs
    static public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> ...annotations) {
        return getAnnotatedClasses(Arrays.asList(annotations));
    }

    /**
     * Returns all classes within a package.
     * @param packageName The root package to search.
     * @return All the classes within a package.
     */
    static public Set<Class<?>> getAllClasses(String packageName) {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo().whitelistPackages(packageName).scan()) {
            return scanResult.getAllClasses().stream()
                    .map((ClassInfo::loadClass))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Function which will be invoked for deciding to include the class in final results.
     */
    @FunctionalInterface
    public interface FilterExpression {
        public boolean include(Class clazz);
    }
}
