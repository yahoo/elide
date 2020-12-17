package com.yahoo.elide.core.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

public interface Method {
    int getModifiers();

    boolean isAnnotationPresent(Class<? extends Annotation> annotation);

    Object invoke(Object obj, Object... args) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException;

    Type<?> getReturnType();
}
