/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

/**
 * InMemoryFilterOperation.
 */
public class InMemoryFilterOperation implements FilterOperation<Set<Predicate>> {
    private final RequestScope requestScope;

    public InMemoryFilterOperation(RequestScope requestScope) {
        this.requestScope = requestScope;
    }

    @Override
    public Set<Predicate> apply(FilterPredicate filterPredicate) {
        return Collections.singleton(this.applyOperator(filterPredicate));
    }

    private Predicate applyOperator(FilterPredicate filterPredicate) {
        return filterPredicate.apply(requestScope);
    }

    public EntityDictionary getDictionary() {
        return requestScope.getDictionary();
    }
}
