/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * POSTFIX Predicate class.
 */
public class PostfixInsensitivePredicate extends FilterPredicate {

    public PostfixInsensitivePredicate(Path path, List<Object> values) {
        super(path, Operator.POSTFIX_CASE_INSENSITIVE, values);
    }

    @SafeVarargs
    public <T> PostfixInsensitivePredicate(Path path, T... a) {
        this(path, Arrays.asList(a));
    }

    public PostfixInsensitivePredicate(PathElement pathElement, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), values);
    }

    @SafeVarargs
    public <T> PostfixInsensitivePredicate(PathElement pathElement, T... a) {
        this(pathElement, Arrays.asList(a));
    }
}
