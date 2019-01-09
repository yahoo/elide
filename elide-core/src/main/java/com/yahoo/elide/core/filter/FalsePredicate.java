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
