/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * IN Predicate class.
 */
public class InPredicate extends FilterPredicate {

    public InPredicate(Path path, List<Object> values) {
        super(path, Operator.IN, values);
    }

    @SafeVarargs
    public <T> InPredicate(Path path, T... a) {
        this(path, Arrays.asList(a));
    }

    public InPredicate(PathElement pathElement, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), values);
    }

    @SafeVarargs
    public <T> InPredicate(PathElement pathElement, T... a) {
        this(pathElement, Arrays.asList(a));
    }
}
