/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils;

import org.glassfish.jersey.server.internal.scanning.AnnotationAcceptingListener;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scans a package for classes by looking at files in the classpath.
 */
public class ClassScanner {
    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     * @param pckg package name
     * @param annotation Annotation to search
     * @return The classes
     */
    static public List<Class<?>> getAnnotatedClasses(Package pckg, Class<? extends Annotation> annotation) {
        final AnnotationAcceptingListener annotationAcceptingListener = new AnnotationAcceptingListener(annotation);
        try (final PackageNamesScanner scanner = new PackageNamesScanner(new String[] { pckg.getName() }, true)) {
            while (scanner.hasNext()) {
                final String next = scanner.next();
                if (annotationAcceptingListener.accept(next)) {
                    try (final InputStream in = scanner.open()) {
                        annotationAcceptingListener.process(next, in);
                    } catch (IOException e) {
                        throw new RuntimeException("AnnotationAcceptingListener failed to process scanned resource: "
                                + next);
                    }
                }
            }
        }

        return annotationAcceptingListener.getAnnotatedClasses()
                .stream().collect(Collectors.toCollection(ArrayList::new));
    }
}
