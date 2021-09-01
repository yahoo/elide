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
public class DefaultClassScanner implements ClassScanner {

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

    private final Map<String, Set<Class<?>>> STARTUP_CACHE = new HashMap<>();

    /**
     * Primarily for tests so builds don't take forever.
     */
    private static DefaultClassScanner _instance;

    public DefaultClassScanner() {
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().enableAnnotationInfo().scan()) {
            for (String annotationName : CACHE_ANNOTATIONS) {
                STARTUP_CACHE.put(annotationName, scanResult.getClassesWithAnnotation(annotationName)
                        .stream()
                        .map(ClassInfo::loadClass)
                        .collect(Collectors.toSet()));

            }
        }
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(Package toScan, Class<? extends Annotation> annotation) {
        return getAnnotatedClasses(toScan.getName(), annotation);
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(String packageName, Class<? extends Annotation> annotation) {
        return STARTUP_CACHE.get(annotation.getCanonicalName()).stream()
                .filter(clazz ->
                        clazz.getPackage().getName().equals(packageName)
                                || clazz.getPackage().getName().startsWith(packageName + "."))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(List<Class<? extends Annotation>> annotations,
            FilterExpression filter) {
        Set<Class<?>> result = new HashSet<>();

        for (Class<? extends Annotation> annotation : annotations) {
            result.addAll(STARTUP_CACHE.get(annotation.getCanonicalName()).stream()
                    .filter(filter::include)
                    .collect(Collectors.toSet()));
        }

        return result;
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(List<Class<? extends Annotation>> annotations) {
        return getAnnotatedClasses(annotations, clazz -> true);
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> ...annotations) {
        return getAnnotatedClasses(Arrays.asList(annotations));
    }

    @Override
    public Set<Class<?>> getAllClasses(String packageName) {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo().whitelistPackages(packageName).scan()) {
            return scanResult.getAllClasses().stream()
                    .map((ClassInfo::loadClass))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Primarily for tests to only create a single instance of this to reduce build times.  Production code
     * will use DI to accomplish the same.
     * @return The single instance.
     */
    public static synchronized DefaultClassScanner getInstance() {
        if (_instance == null) {
            _instance = new DefaultClassScanner();
        }
        return _instance;
    }
}
