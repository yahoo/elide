/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import java.lang.annotation.Annotation;

public interface Package {
    <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass);
    String getName();

    Package getParentPackage();
}
