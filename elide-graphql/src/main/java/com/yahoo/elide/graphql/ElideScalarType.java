/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ElideScalarType {
    Class<?> type();
    String name();
    String description() default "Custom Elide Scalar type";
    Class<?> usesSerdeOfType() default Void.class;  //This type will be used to register Serde in CoerceUtil
                                                    //Keep default Void value if no serde is used or already registered
}
