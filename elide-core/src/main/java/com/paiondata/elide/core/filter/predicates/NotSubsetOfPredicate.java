/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.predicates;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.Path.PathElement;
import com.paiondata.elide.core.filter.Operator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NotSubsetOf Predicate class.
 * <p>
 * This determines if a collection association path is not a subset of all
 * values in the collection.
 * <p>
 * The negation of this is {@link SubsetOfPredicate}.
 */
public class NotSubsetOfPredicate extends FilterPredicate {

    public NotSubsetOfPredicate(Path path, List<Object> values) {
        super(path, Operator.NOTSUBSETOF, values);
    }

    @SafeVarargs
    public <T> NotSubsetOfPredicate(Path path, T... a) {
        this(path, Arrays.asList(a));
    }

    public NotSubsetOfPredicate(PathElement pathElement, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), values);
    }

    @SafeVarargs
    public <T> NotSubsetOfPredicate(PathElement pathElement, T... a) {
        this(pathElement, Arrays.asList(a));
    }
}
