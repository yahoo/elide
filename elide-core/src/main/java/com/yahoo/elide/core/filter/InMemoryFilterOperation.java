/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * InMemoryFilterOperation
 */
public class InMemoryFilterOperation implements FilterOperation<Set<java.util.function.Predicate>> {
    private final EntityDictionary dictionary;

    public InMemoryFilterOperation(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public Set<java.util.function.Predicate> apply(FilterPredicate filterPredicate) {
        return Collections.singleton(this.applyOperator(filterPredicate));
    }

    @Override
    public Set<java.util.function.Predicate> applyAll(Set<FilterPredicate> filterPredicates) {
        return filterPredicates.stream()
                .map(this::applyOperator)
                .collect(Collectors.toSet());
    }

    private java.util.function.Predicate applyOperator(FilterPredicate filterPredicate) {
        return filterPredicate.apply(dictionary);
    }

    public EntityDictionary getDictionary() {
        return dictionary;
    }
}
