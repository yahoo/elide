/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;

import java.util.Collection;
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
    public Set<java.util.function.Predicate> apply(Predicate predicate) {
        return Collections.singleton(predicate.getOperator().apply(predicate, dictionary));
    }

    @Override
    public Set<java.util.function.Predicate> applyAll(Set<Predicate> predicates) {
        return predicates.stream()
                .map(this::apply)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public EntityDictionary getDictionary() {
        return dictionary;
    }
}
