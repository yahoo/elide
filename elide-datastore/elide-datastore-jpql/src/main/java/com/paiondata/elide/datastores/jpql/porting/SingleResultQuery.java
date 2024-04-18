/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpql.porting;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link Query} with a single result.
 */
public class SingleResultQuery implements Query {
    private final Supplier<Object> result;

    public SingleResultQuery(Supplier<Object> result) {
        this.result = result;
    }

    @Override
    public Query setFirstResult(int num) {
        return this;
    }

    @Override
    public Query setMaxResults(int num) {
        return this;
    }

    @Override
    public Query setParameter(String name, Object value) {
        return this;
    }

    @Override
    public Query setParameterList(String name, Collection<?> values) {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T uniqueResult() {
        return (T) this.result.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Iterable<T> scroll() {
        T object = (T) result.get();
        return object != null ? List.of(object) : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Iterable<T> list() {
        T object = (T) result.get();
        return object != null ? List.of(object) : Collections.emptyList();
    }
}
