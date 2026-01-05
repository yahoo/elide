/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.graal;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.utils.ClassScannerCache;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Native Image Feature for Elide.
 */
public class ElideFeature implements Feature {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Map<String, Set<Class<?>>> classes = ClassScannerCache.getInstance();
        // In GraalVM 21+ java.class.path is not set so need to use
        // access.getApplicationClassPath otherwise no classes are found
        try (ScanResult scanResult = new ClassGraph().overrideClasspath(access.getApplicationClassPath())
                .enableClassInfo().enableAnnotationInfo().scan()) {
            for (String annotationName : ClassScannerCache.getCachedAnnotations()) {
                Set<Class<?>> value = scanResult.getClassesWithAnnotation(annotationName).stream()
                        .map(ClassInfo::loadClass).collect(Collectors.toCollection(LinkedHashSet::new));
                if (!value.isEmpty()) {
                    classes.put(annotationName, value);
                }
            }
        }
        Set<Class<?>> results = new LinkedHashSet<>();
        classes.values().stream().forEach(set -> set.stream().forEach(clazz -> {
            results.add(clazz);
            LifeCycleHookBinding lifeCycleHookBinding = clazz.getAnnotation(LifeCycleHookBinding.class);
            if (lifeCycleHookBinding != null) {
                results.add(lifeCycleHookBinding.hook());
            }
        }));
        Set<Class<?>> hooks = new LinkedHashSet<>();
        results.forEach(clazz -> {
            for (Field field : clazz.getFields()) {
                LifeCycleHookBinding lifeCycleHookBinding = field.getAnnotation(LifeCycleHookBinding.class);
                if (lifeCycleHookBinding != null) {
                    hooks.add(lifeCycleHookBinding.hook());
                }
            }
            for (Method method : clazz.getMethods()) {
                LifeCycleHookBinding lifeCycleHookBinding = method.getAnnotation(LifeCycleHookBinding.class);
                if (lifeCycleHookBinding != null) {
                    hooks.add(lifeCycleHookBinding.hook());
                }
            }
        });
        results.addAll(hooks);
        // Sort
        List<Class<?>> ordered = new ArrayList<>(results);
        ordered.sort((left, right) -> {
            return left.getName().compareTo(right.getName());
        });
        ordered.forEach(this::register);
    }

    void register(Class<?> clazz) {
        System.out.println("Elide registering " + clazz + " for reflection");

        RuntimeReflection.register(clazz);
        for (Field field : clazz.getFields()) {
            RuntimeReflection.register(field);
        }

        for (Method method : clazz.getMethods()) {
            RuntimeReflection.register(method);
        }

        for (Constructor<?> constructor : clazz.getConstructors()) {
            RuntimeReflection.register(constructor);
        }

        for (Field field : clazz.getDeclaredFields()) {
            RuntimeReflection.register(field);
        }

        for (Method method : clazz.getDeclaredMethods()) {
            RuntimeReflection.register(method);
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            RuntimeReflection.register(constructor);
        }
    }
}
