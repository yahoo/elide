/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dynamic;

import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.ParameterizedModel;
import com.yahoo.elide.core.type.Type;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

public class FieldType implements Field {
    private Map<Class<? extends Annotation>, Annotation> annotations;
    private String name;
    private Type type;

    public FieldType(String name, Type type, Map<Class<? extends Annotation>, Annotation> annotations) {
        this.name = name;
        this.annotations = annotations;
        this.type = type;
    }
    @Override
    public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if (! ParameterizedModel.class.isAssignableFrom(obj.getClass())) {
            throw new IllegalArgumentException("Class is not a dynamic type: " + obj.getClass());
        }

        ParameterizedModel model = (ParameterizedModel) obj;

        return model.invoke(Attribute.builder()
                .name(name)
                .type(type)
                .alias(name)
                .build());
    }

    @Override
    public Type<?> getType() {
        return type;
    }

    @Override
    public Type<?> getParameterizedType(Type<?> parentType, Optional<Integer> index) {
        return type;
    }

    @Override
    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return annotations.containsKey(annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotations.containsKey(annotationClass)) {
            return (T) annotations.get(annotationClass);
        }
        return null;
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        if (annotations.containsKey(annotationClass)) {
            Annotation[] result = new Annotation[1];
            result[0] = annotations.get(annotationClass);
            return (T[]) result;
        }
        return null;
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations.values().toArray(new Annotation[0]);
    }

    @Override
    public int getModifiers() {
        return 0;
    }
}
