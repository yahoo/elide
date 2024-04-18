/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.predicates;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.Path.PathElement;
import com.paiondata.elide.core.filter.Operator;

import java.util.Collections;

/**
 * False Predicate class.
 */
public class FalsePredicate extends FilterPredicate {

    public FalsePredicate(Path path) {
        super(path, Operator.FALSE, Collections.emptyList());
    }

    public FalsePredicate(PathElement pathElement) {
        this(new Path(Collections.singletonList(pathElement)));
    }
}
