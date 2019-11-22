/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Scans a package for classes by looking at files in the classpath.
 */
public class ClassScanner {
    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param toScan     package to scan
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
     * @param annotation  Annotation to search
     * @return The classes
     */
    static public Set<Class<?>> getAnnotatedClasses(String packageName, Class<? extends Annotation> annotation) {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        configurationBuilder.addUrls(ClasspathHelper.forPackage(packageName));
        configurationBuilder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());

        Reflections reflections = new Reflections(configurationBuilder);

        return reflections.getTypesAnnotatedWith(annotation, true);
    }
}
