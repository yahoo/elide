package com.yahoo.elide.core.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

public interface Type<T> {
    String getSimpleName();
    String getName();
    Method getMethod(String name, Type<?>... parameterTypes) throws NoSuchMethodException;
    Type<?> getSuperclass();
    <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass);
    <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass);
    <A extends Annotation> A getAnnotation(Class<A> annotationClass);
    boolean isAssignableFrom(Type<?> cls);
    Package getPackage();
    Method[] getMethods();
    Method[] getDeclaredMethods();
    Field[] getFields();
    Field[] getDeclaredFields();
    Method[] getConstructors();
    default boolean isParameterized() {
        return false;
    }
    boolean hasSuperType();
    T newInstance() throws InstantiationException, IllegalAccessException;
}
