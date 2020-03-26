/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans a package for classes by looking at files in the classpath.
 */
public class ClassScanner {
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
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo().enableAnnotationInfo().whitelistPackages(packageName).scan()) {
            return scanResult.getClassesWithAnnotation(annotation.getCanonicalName()).stream()
                    .map((ClassInfo::loadClass))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the current class loader.
     *
     * @param annotation Annotation to search
     * @return The classes
     */
    static public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo().enableAnnotationInfo().scan()) {
            return scanResult.getClassesWithAnnotation(annotation.getCanonicalName()).stream()
                    .map((ClassInfo::loadClass))
                    .collect(Collectors.toSet());
        }
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
}
