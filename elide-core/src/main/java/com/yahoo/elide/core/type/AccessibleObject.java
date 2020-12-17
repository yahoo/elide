package com.yahoo.elide.core.type;

import java.lang.annotation.Annotation;

public interface AccessibleObject extends Member {
    default void setAccessible(boolean flag) {
        //NOOP
    }

    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);
}
