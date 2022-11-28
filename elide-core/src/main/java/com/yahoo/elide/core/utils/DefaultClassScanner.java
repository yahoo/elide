/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
            Include.class.getCanonicalName(),
            SecurityCheck.class.getCanonicalName(),
            ElideTypeConverter.class.getCanonicalName(),

            //GraphQL annotations.  Strings here to avoid dependency.
            "com.yahoo.elide.graphql.subscriptions.annotations.Subscription",

            //Aggregation Store Annotations.  Strings here to avoid dependency.
            "com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable",
            "com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery",
            "org.hibernate.annotations.Subselect",

            //JPA
            "jakarta.persistence.Entity",
            "jakarta.persistence.Table"
    };

    private final Map<String, Set<Class<?>>> startupCache;

    /**
     * Primarily for tests so builds don't take forever.
     */
    private static DefaultClassScanner _instance;

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
        this.startupCache = new HashMap<>();
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().enableAnnotationInfo().scan()) {
            for (String annotationName : CACHE_ANNOTATIONS) {
                startupCache.put(annotationName, scanResult.getClassesWithAnnotation(annotationName)
                        .stream()
                        .map(ClassInfo::loadClass)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));

            }
        }
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(Package toScan, Class<? extends Annotation> annotation) {
        return getAnnotatedClasses(toScan.getName(), annotation);
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(String packageName, Class<? extends Annotation> annotation) {
        return startupCache.get(annotation.getCanonicalName()).stream()
                .filter(clazz ->
                        clazz.getPackage().getName().equals(packageName)
                                || clazz.getPackage().getName().startsWith(packageName + "."))
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
                .enableClassInfo().whitelistPackages(packageName).scan()) {
            return scanResult.getAllClasses().stream()
                    .map((ClassInfo::loadClass))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
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
