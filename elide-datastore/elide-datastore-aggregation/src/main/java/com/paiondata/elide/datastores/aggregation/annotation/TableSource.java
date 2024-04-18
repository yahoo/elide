/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

import static com.paiondata.elide.datastores.aggregation.dynamic.NamespacePackage.DEFAULT;

/**
 * The definition of TableSource.
 */
public @interface TableSource {
    String table();
    String namespace() default DEFAULT;
    String column();
    String [] suggestionColumns() default {};
}
