/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dynamic;

import static java.lang.reflect.Modifier.PUBLIC;

import com.yahoo.elide.core.exceptions.InvalidParameterizedAttributeException;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.ParameterizedModel;
import com.yahoo.elide.core.type.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Optional;

/**
 * A dynamic Elide model field that wraps a deserialized HJSON measure or dimension.
 */
public class FieldType implements Field {
    private static final long serialVersionUID = -1358950447581934754L;
    private transient Map<Class<? extends Annotation>, Annotation> annotations;
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

        try {
            return model.invoke(Attribute.builder()
                    .name(name)
                    .type(type)
                    .alias(name)
                    .build());
        } catch (InvalidParameterizedAttributeException e) {

            //Return default value if the field has not been set.
            return null;
        }
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
        if (! ParameterizedModel.class.isAssignableFrom(obj.getClass())) {
            throw new IllegalArgumentException("Class is not a dynamic type: " + obj.getClass());
        }

        ParameterizedModel model = (ParameterizedModel) obj;

        model.addAttributeValue(Attribute.builder().name(name).type(type).alias(name).build(), value);
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
            T[] result = (T[]) Array.newInstance(annotationClass, 1);
            result[0] = (T) annotations.get(annotationClass);
            return result;
        }
        return (T[]) Array.newInstance(annotationClass, 0);
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
        return PUBLIC;
    }
}
