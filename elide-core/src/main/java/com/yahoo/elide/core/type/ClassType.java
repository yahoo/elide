/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An elide type that wraps a Java class.
 * @param <T> The Java class.
 */
public class ClassType<T> implements Type<T> {
    public static final List<Method> OBJ_METHODS = ImmutableList.copyOf(
            Arrays.stream(Object.class.getMethods()).map(ClassType::constructMethod).collect(Collectors.toList()));

    public static final ClassType MAP_TYPE = new ClassType<>(Map.class);
    public static final ClassType COLLECTION_TYPE = new ClassType<>(Collection.class);
    public static final ClassType STRING_TYPE = new ClassType<>(String.class);
    public static final ClassType BOOLEAN_TYPE = new ClassType<>(Boolean.class);
    public static final ClassType LONG_TYPE = new ClassType<>(Long.class);
    public static final ClassType BIGDECIMAL_TYPE = new ClassType<>(BigDecimal.class);
    public static final ClassType NUMBER_TYPE = new ClassType<>(Number.class);
    public static final ClassType DATE_TYPE = new ClassType<>(Date.class);
    public static final ClassType OBJECT_TYPE = new ClassType<>(Object.class);
    public static final ClassType CLASS_TYPE = new ClassType<>(Class.class);
    public static final ClassType INTEGER_TYPE = new ClassType<>(Integer.class);

    @Getter
    private Class<T> cls;

    /**
     * Constructor.
     * @param cls The underlying java class.
     */
    public ClassType(Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public String getCanonicalName() {
        return cls.getCanonicalName();
    }

    @Override
    public String getSimpleName() {
        return cls.getSimpleName();
    }

    @Override
    public String getName() {
        return cls.getName();
    }

    @Override
    public Type<T> getSuperclass() {
        Class<?> superClass = cls.getSuperclass();
        if (superClass == null) {
            return null;
        }

        return new ClassType(superClass);
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        return cls.getAnnotationsByType(annotationClass);
    }

    @Override
    public Field[] getFields() {
        return Arrays.stream(cls.getFields())
                .map(ClassType::constructField).collect(Collectors.toList()).toArray(new Field[0]);
    }

    @Override
    public Field[] getDeclaredFields() {
        return Arrays.stream(cls.getDeclaredFields())
                .map(ClassType::constructField).collect(Collectors.toList()).toArray(new Field[0]);
    }

    @Override
    public Field getDeclaredField(String name) throws NoSuchFieldException {
        return constructField(cls.getDeclaredField(name));
    }

    @Override
    public Method[] getConstructors() {
        return Arrays.stream(cls.getConstructors())
                .map(ClassType::constructMethod).collect(Collectors.toList()).toArray(new Method[0]);
    }

    @Override
    public boolean isParameterized() {
        return (cls.getGenericSuperclass() instanceof ParameterizedType);
    }

    @Override
    public boolean hasSuperType() {
        return cls != null && cls != Object.class;
    }

    @Override
    public T newInstance() throws InstantiationException, IllegalAccessException {
        return cls.newInstance();
    }

    @Override
    public Package getPackage() {
        return constructPackage(cls.getPackage());
    }

    @Override
    public Method[] getMethods() {
        return Arrays.stream(cls.getMethods())
                .map(ClassType::constructMethod).collect(Collectors.toList()).toArray(new Method[0]);
    }

    @Override
    public Method[] getDeclaredMethods() {
        return Arrays.stream(cls.getDeclaredMethods())
                .map(ClassType::constructMethod).collect(Collectors.toList()).toArray(new Method[0]);
    }

    @Override
    public boolean isAssignableFrom(Type other) {
        Optional<Class<?>> clazz = other.getUnderlyingClass();

        if (! clazz.isPresent()) {
            return false;
        }

        return cls.isAssignableFrom(clazz.get());
    }

    @Override
    public boolean isPrimitive() {
        return cls.isPrimitive();
    }

    @Override
    public Annotation getAnnotation(Class annotationClass) {
        return cls.getAnnotation(annotationClass);
    }

    @Override
    public Annotation getDeclaredAnnotation(Class annotationClass) {
        return cls.getDeclaredAnnotation(annotationClass);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    @Override
    public Method getMethod(String name, Type[] parameterTypes)  throws NoSuchMethodException, SecurityException {
        Class<?>[] typeParams = Arrays.stream(parameterTypes)
                .map(ClassType.class::cast)
                .map(classType -> (classType == null) ? null : classType.getCls())
                .collect(Collectors.toList()).toArray(new Class[0]);

        return constructMethod(cls.getMethod(name, typeParams));
    }

    private static Field constructField(java.lang.reflect.Field field) {
        if (field == null) {
            return null;
        }

        return new FieldType(field);
    }

    private static Method constructMethod(java.lang.reflect.Executable method) {
        if (method == null) {
            return null;
        }

        return new MethodType(method);
    }

    private static Package constructPackage(java.lang.Package pkg) {
        if (pkg == null) {
            return null;
        }

        return new Package() {
            @Override
            public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
                return pkg.getDeclaredAnnotation(annotationClass);
            }

            @Override
            public String getName() {
                return pkg.getName();
            }

            @Override
            public Package getParentPackage() {
                String name = pkg.getName();
                int idx = name.lastIndexOf('.');
                return idx == -1 ? null : constructPackage(java.lang.Package.getPackage(name.substring(0, idx)));
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassType<?> classType = (ClassType<?>) o;
        return Objects.equals(cls, classType.cls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cls);
    }

    @Override
    public String toString() {
        return "ClassType{" + "cls=" + cls + '}';
    }

    @Override
    public boolean isEnum() {
        return cls.isEnum();
    }

    @Override
    public T[] getEnumConstants() {
        return cls.getEnumConstants();
    }

    @Override
    public Optional<Class<T>> getUnderlyingClass() {
        return Optional.of(getCls());
    }

    /**
     * Construction helper.
     * @param <T> class type
     * @param cls The underlying java class.
     * @return wrapped Type
     */
    public static <T> ClassType<T> of(Class<T> cls) {
        return cls == null ? null : new ClassType<>(cls);
    }
}
