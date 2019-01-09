/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;

import java.util.Collections;

/**
 * GT Predicate class.
 */
public class GTPredicate extends FilterPredicate {

    public GTPredicate(Path path, Object value) {
        super(path, Operator.GT, Collections.singletonList(value));
    }

    public GTPredicate(PathElement pathElement, Object value) {
        this(new Path(Collections.singletonList(pathElement)), value);
    }
}
