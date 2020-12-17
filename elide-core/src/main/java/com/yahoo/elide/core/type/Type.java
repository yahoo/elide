package com.yahoo.elide.core.type;

import java.lang.annotation.Annotation;

public interface Type<T> {
    String getSimpleName();
    String getName();
    Method getMethod(String name, Type<?>... parameterTypes)  throws NoSuchMethodException, SecurityException;
    Type<?> getSuperclass();
    <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass);
    <A extends Annotation> A getAnnotation(Class<A> annotationClass);
    Field[] getDeclaredFields();
    boolean isAssignableFrom(Type<?> cls);
    Package getPackage();
}
