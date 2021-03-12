/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.type;

import org.apache.commons.lang3.reflect.TypeUtils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

/**
 * Elide Method that wraps a Java Method.
 */
@AllArgsConstructor
@EqualsAndHashCode
public class MethodType implements Method {

    private java.lang.reflect.Executable method;

    @Override
    public int getModifiers() {
        return method.getModifiers();
    }

    @Override
    public void setAccessible(boolean flag) {
        method.setAccessible(flag);
    }

    @Override
    public boolean isSynthetic() {
        return method.isSynthetic();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return method.getAnnotationsByType(annotationClass);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return method.getDeclaredAnnotations();
    }

    @Override
    public Annotation[] getAnnotations() {
        return method.getAnnotations();
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public int getParameterCount() {
        return method.getParameterCount();
    }

    @Override
    public Object invoke(Object obj, Object... args) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        if (! (method instanceof java.lang.reflect.Method)) {
            throw new UnsupportedOperationException("Constructors cannot be invoked");
        }
        return ((java.lang.reflect.Method) method).invoke(obj, args);
    }

    @Override
    public Type<?> getReturnType() {
        if (! (method instanceof java.lang.reflect.Method)) {
            throw new UnsupportedOperationException("Constructors cannot be invoked");
        }
        return ClassType.of(((java.lang.reflect.Method) method).getReturnType());
    }

    @Override
    public Type<?> getParameterizedReturnType(Type<?> parentType, Optional<Integer> index) {
        if (! (method instanceof java.lang.reflect.Method)) {
            throw new UnsupportedOperationException("Constructors cannot be invoked");
        }

        if (parentType instanceof Dynamic) {
            return getReturnType();
        }

        java.lang.reflect.Type type = ((java.lang.reflect.Method) method).getGenericReturnType();

        if (type instanceof ParameterizedType && index.isPresent()) {
            return ClassType.of(
                    TypeUtils.getRawType(
                            ((ParameterizedType) type).getActualTypeArguments()[index.get().intValue()],
                            ((ClassType) parentType).getCls()
                    )
            );
        }
        return ClassType.of(TypeUtils.getRawType(type, ((ClassType) parentType).getCls()));
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }
}
