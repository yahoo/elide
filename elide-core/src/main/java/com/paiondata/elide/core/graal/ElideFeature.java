/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.graal;

import com.paiondata.elide.core.utils.ClassScannerCache;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Native Image Feature for Elide.
 */
public class ElideFeature implements Feature {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        ClassScannerCache.getInstance().values().stream().forEach(set -> set.stream().forEach(this::register));
    }

    void register(Class<?>... classes) {
        Arrays.stream(classes).forEach(clazz -> {
            System.out.println("Elide registering class " + clazz + " for reflection");

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
        });
    }
}
