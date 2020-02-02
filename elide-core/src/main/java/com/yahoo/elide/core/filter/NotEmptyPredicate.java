/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;

import java.util.Collections;

/**
 * Not Empty Predicate Class.
 */
public class NotEmptyPredicate extends FilterPredicate {

    public NotEmptyPredicate(Path path) {
        super(path, Operator.NOTEMPTY, Collections.emptyList());
    }

    public NotEmptyPredicate(Path.PathElement pathElement) {
        super(pathElement, Operator.NOTEMPTY, Collections.emptyList());
    }
}
