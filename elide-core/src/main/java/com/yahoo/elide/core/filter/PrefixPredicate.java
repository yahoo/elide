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
 * PREFIX Predicate class.
 */
public class PrefixPredicate extends FilterPredicate {

    public PrefixPredicate(Path path, List<Object> values) {
        super(path, Operator.PREFIX, values);
    }

    @SafeVarargs
    public <T> PrefixPredicate(Path path, T... a) {
        this(path, Arrays.asList(a));
    }

    public PrefixPredicate(PathElement pathElement, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), values);
    }

    @SafeVarargs
    public <T> PrefixPredicate(PathElement pathElement, T... a) {
        this(pathElement, Arrays.asList(a));
    }
}
