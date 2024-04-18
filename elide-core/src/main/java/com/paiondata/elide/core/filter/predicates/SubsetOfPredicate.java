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
 * SubsetOf Predicate class.
 * <p>
 * This determines if a collection association path is a subset of all values
 * in the collection.
 * <p>
 * The negation of this is {@link NotSubsetOfPredicate}.
 */
public class SubsetOfPredicate extends FilterPredicate {

    public SubsetOfPredicate(Path path, List<Object> values) {
        super(path, Operator.SUBSETOF, values);
    }

    @SafeVarargs
    public <T> SubsetOfPredicate(Path path, T... a) {
        this(path, Arrays.asList(a));
    }

    public SubsetOfPredicate(PathElement pathElement, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), values);
    }

    @SafeVarargs
    public <T> SubsetOfPredicate(PathElement pathElement, T... a) {
        this(pathElement, Arrays.asList(a));
    }
}
