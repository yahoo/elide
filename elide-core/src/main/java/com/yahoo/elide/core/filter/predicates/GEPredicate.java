/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.predicates;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.filter.Operator;

import java.util.Collections;

/**
 * GE Predicate class.
 */
public class GEPredicate extends FilterPredicate {

    public GEPredicate(Path path, Object value) {
        super(path, Operator.GE, Collections.singletonList(value));
    }

    public GEPredicate(PathElement pathElement, Object value) {
        this(new Path(Collections.singletonList(pathElement)), value);
    }
}
