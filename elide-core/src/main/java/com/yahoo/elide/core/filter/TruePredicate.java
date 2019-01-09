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
 * TRUE Predicate class.
 */
public class TruePredicate extends FilterPredicate {

    public TruePredicate(Path path) {
        super(path, Operator.TRUE, Collections.emptyList());
    }

    public TruePredicate(PathElement pathElement) {
        this(new Path(Collections.singletonList(pathElement)));
    }
}
