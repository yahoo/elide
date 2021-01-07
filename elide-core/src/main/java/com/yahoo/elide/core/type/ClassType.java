/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import org.apache.commons.lang3.reflect.TypeUtils;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassType<T> implements Type<T> {

    public static final ClassType MAP_TYPE = new ClassType(Map.class);
    public static final ClassType COLLECTION_TYPE = new ClassType(Collection.class);
    public static final ClassType STRING_TYPE = new ClassType(String.class);
    public static final ClassType NUMBER_TYPE = new ClassType(Number.class);
    public static final ClassType DATE_TYPE = new ClassType(Date.class);
    public static final ClassType OBJECT_TYPE = new ClassType(Object.class);
    public static final ClassType CLASS_TYPE = new ClassType(Class.class);
    public static final ClassType INTEGER_TYPE = new ClassType(Integer.class);

    @Getter
    private Class<T> cls;

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
        if (other instanceof DynamicType) {
            return false;
        }

        return cls.isAssignableFrom(((ClassType) other).getCls());
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

    public static Field constructField(java.lang.reflect.Field field) {
        if (field == null) {
            return null;
        }

        return new Field() {

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
                return new ClassType(field.getType());
            }

            @Override
            public Type<?> getParameterizedType(Type<?> parentType, Optional<Integer> index) {
                if (parentType instanceof DynamicType) {
                    return getType();
                }

                java.lang.reflect.Type type = field.getGenericType();

                if (type instanceof ParameterizedType && index.isPresent()) {
                    return new ClassType(
                            TypeUtils.getRawType(
                                ((ParameterizedType) type).getActualTypeArguments()[index.get().intValue()],
                                ((ClassType) parentType).getCls()
                            )
                    );
                }
                return new ClassType(TypeUtils.getRawType(type, ((ClassType) parentType).getCls()));
            }

            @Override
            public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
                field.set(obj, value);
            }

        };
    }

    public static Method constructMethod(java.lang.reflect.Executable method) {
        if (method == null) {
            return null;
        }

        return new Method() {
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
                return new ClassType(((java.lang.reflect.Method) method).getReturnType());
            }

            @Override
            public Type<?> getParameterizedReturnType(Type<?> parentType, Optional<Integer> index) {
                if (! (method instanceof java.lang.reflect.Method)) {
                    throw new UnsupportedOperationException("Constructors cannot be invoked");
                }

                if (parentType instanceof DynamicType) {
                    return getReturnType();
                }

                java.lang.reflect.Type type = ((java.lang.reflect.Method) method).getGenericReturnType();

                if (type instanceof ParameterizedType && index.isPresent()) {
                    return new ClassType(
                            TypeUtils.getRawType(
                                    ((ParameterizedType) type).getActualTypeArguments()[index.get().intValue()],
                                    ((ClassType) parentType).getCls()
                            )
                    );
                }
                return new ClassType(TypeUtils.getRawType(type, ((ClassType) parentType).getCls()));
            }

            @Override
            public Class<?>[] getParameterTypes() {
                return method.getParameterTypes();
            }
        };
    }

    public static Package constructPackage(java.lang.Package pkg) {
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
}
