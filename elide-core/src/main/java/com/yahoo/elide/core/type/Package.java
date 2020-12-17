package com.yahoo.elide.core.type;

import java.lang.annotation.Annotation;

public interface Package {
    <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass);
    String getName();

    Package getParentPackage();
}
