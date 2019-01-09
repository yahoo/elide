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
 * LE Predicate class.
 */
public class LEPredicate extends FilterPredicate {

    public LEPredicate(Path path, Object value) {
        super(path, Operator.LE, Collections.singletonList(value));
    }

    public LEPredicate(PathElement pathElement, Object value) {
        this(new Path(Collections.singletonList(pathElement)), value);
    }
}
