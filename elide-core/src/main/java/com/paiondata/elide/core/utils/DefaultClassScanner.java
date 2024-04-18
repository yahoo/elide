/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;


import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans a package for classes by looking at files in the classpath.
 */
public class DefaultClassScanner implements ClassScanner {

    private final Map<String, Set<Class<?>>> startupCache;

    /**
     * For use within a container where class scanning happens at compile time.
     * @param startupCache Maps annotations (in CACHE_ANNOTATIONS) to classes.
     */
    public DefaultClassScanner(Map<String, Set<Class<?>>> startupCache) {
        this.startupCache = startupCache;
    }

    /**
     * For use within a container where class scanning happens at boot time.
     */
    public DefaultClassScanner() {
        this.startupCache = ClassScannerCache.getInstance();
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(Package toScan, Class<? extends Annotation> annotation) {
        return getAnnotatedClasses(toScan.getName(), annotation);
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(String packageName, Class<? extends Annotation> annotation) {
        LinkedHashSet<Class<?>> annotatedClasses = startupCache.get(annotation.getCanonicalName()).stream()
                .filter(clazz -> clazz.getPackage().getName().equals(packageName)
                        || clazz.getPackage().getName().startsWith(packageName + "."))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Check if the annotated class collection obtained is empty
        if (annotatedClasses.isEmpty()) {
            throw new IllegalArgumentException("No annotated classes found in the specified package: " + packageName);
        }

        return annotatedClasses;
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(List<Class<? extends Annotation>> annotations,
            FilterExpression filter) {
        Set<Class<?>> result = new LinkedHashSet<>();

        for (Class<? extends Annotation> annotation : annotations) {
            result.addAll(startupCache.get(annotation.getCanonicalName()).stream()
                    .filter(filter::include)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        return result;
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(List<Class<? extends Annotation>> annotations) {
        return getAnnotatedClasses(annotations, clazz -> true);
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> ... annotations) {
        return getAnnotatedClasses(Arrays.asList(annotations));
    }

    @Override
    public Set<Class<?>> getAllClasses(String packageName) {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo().acceptPackages(packageName).scan()) {
            return scanResult.getAllClasses().stream()
                    .map((ClassInfo::loadClass))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
