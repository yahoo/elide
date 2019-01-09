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
 * ISNULL Predicate class.
 */
public class IsNullPredicate extends FilterPredicate {

    public IsNullPredicate(Path path) {
        super(path, Operator.ISNULL, Collections.emptyList());
    }

    public IsNullPredicate(PathElement pathElement) {
        this(new Path(Collections.singletonList(pathElement)));
    }
}
