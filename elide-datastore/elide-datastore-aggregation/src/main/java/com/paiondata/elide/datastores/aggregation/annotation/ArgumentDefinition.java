/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

import com.paiondata.elide.datastores.aggregation.metadata.enums.ValueType;

/**
 * The definition of Argument.
 */
public @interface ArgumentDefinition {
    String name() default "";
    String description() default "";
    ValueType type() default ValueType.TEXT;
    TableSource tableSource() default @TableSource(table = "", column = "");
    String [] values() default {};
    String defaultValue() default "";
}
