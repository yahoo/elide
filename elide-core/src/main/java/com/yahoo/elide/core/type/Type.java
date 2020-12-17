package com.yahoo.elide.core.type;

import java.lang.annotation.Annotation;

public interface Type<T> {
    String getSimpleName();
    String getName();
    Method getMethod(String name, Type<?>... parameterTypes) throws NoSuchMethodException;
    Type<?> getSuperclass();
    <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass);
    <A extends Annotation> A getAnnotation(Class<A> annotationClass);
    boolean isAssignableFrom(Type<?> cls);
    Package getPackage();
    Method[] getMethods();
    Method[] getDeclaredMethods();
    Field[] getFields();
    Field[] getDeclaredFields();
}
