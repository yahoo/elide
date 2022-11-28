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
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

/**
 * Elide field that wraps a Java field.
 */
@AllArgsConstructor
@EqualsAndHashCode
public class FieldType implements Field {
    private static final long serialVersionUID = -1949519786163885434L;

    private java.lang.reflect.Field field;

    @Override
    public void setAccessible(boolean flag) {
        field.setAccessible(flag);
    }

    @Override
    public boolean isSynthetic() {
        return field.isSynthetic();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return field.isAnnotationPresent(annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return field.getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return field.getAnnotationsByType(annotationClass);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return field.getDeclaredAnnotations();
    }

    @Override
    public Annotation[] getAnnotations() {
        return field.getAnnotations();
    }

    @Override
    public int getModifiers() {
        return field.getModifiers();
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.get(obj);
    }

    @Override
    public Type<?> getType() {
        return ClassType.of(field.getType());
    }

    @Override
    public Type<?> getParameterizedType(Type<?> parentType, Optional<Integer> index) {
        if (parentType instanceof Dynamic) {
            return getType();
        }

        java.lang.reflect.Type type = field.getGenericType();

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
    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        field.set(obj, value);
    }
}
