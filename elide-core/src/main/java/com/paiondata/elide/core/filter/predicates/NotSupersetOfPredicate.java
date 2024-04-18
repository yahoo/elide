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
 * NotSuperset Predicate class.
 * <p>
 * This determines if a collection association path is not a superset of all
 * values in the collection.
 * <p>
 * The negation of this is {@link SupersetOfPredicate}.
 */
public class NotSupersetOfPredicate extends FilterPredicate {

    public NotSupersetOfPredicate(Path path, List<Object> values) {
        super(path, Operator.NOTSUPERSETOF, values);
    }

    @SafeVarargs
    public <T> NotSupersetOfPredicate(Path path, T... a) {
        this(path, Arrays.asList(a));
    }

    public NotSupersetOfPredicate(PathElement pathElement, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), values);
    }

    @SafeVarargs
    public <T> NotSupersetOfPredicate(PathElement pathElement, T... a) {
        this(pathElement, Arrays.asList(a));
    }
}
