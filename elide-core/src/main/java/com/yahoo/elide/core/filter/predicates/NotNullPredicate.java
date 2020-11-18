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
 * Not NUL Predicate class.
 */
public class NotNullPredicate extends FilterPredicate {

    public NotNullPredicate(Path path) {
        super(path, Operator.NOTNULL, Collections.emptyList());
    }

    public NotNullPredicate(PathElement pathElement) {
        this(new Path(Collections.singletonList(pathElement)));
    }
}
