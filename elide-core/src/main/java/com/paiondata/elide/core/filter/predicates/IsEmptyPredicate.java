/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.predicates;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.filter.Operator;

import java.util.Collections;

/**
 * Is Empty Predicate Class.
 */
public class IsEmptyPredicate extends FilterPredicate {

    public IsEmptyPredicate(Path path) {
        super(path, Operator.ISEMPTY, Collections.emptyList());
    }

    public IsEmptyPredicate(Path.PathElement pathElement) {
        super(pathElement, Operator.ISEMPTY, Collections.emptyList());
    }
}
