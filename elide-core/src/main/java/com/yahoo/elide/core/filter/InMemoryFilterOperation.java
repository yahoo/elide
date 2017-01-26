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
import java.util.stream.Collectors;

/**
 * InMemoryFilterOperation
 */
public class InMemoryFilterOperation implements FilterOperation<Set<java.util.function.Predicate>> {
    private final RequestScope requestScope;

    public InMemoryFilterOperation(RequestScope requestScope) {
        this.requestScope = requestScope;
    }

    @Override
    public Set<java.util.function.Predicate> apply(Predicate predicate) {
        return Collections.singleton(this.applyOperator(predicate));
    }

    @Override
    public Set<java.util.function.Predicate> applyAll(Set<Predicate> predicates) {
        return predicates.stream()
                .map(this::applyOperator)
                .collect(Collectors.toSet());
    }

    private java.util.function.Predicate applyOperator(Predicate predicate) {
        return predicate.apply(requestScope);
    }

    public EntityDictionary getDictionary() {
        return requestScope.getDictionary();
    }
}
