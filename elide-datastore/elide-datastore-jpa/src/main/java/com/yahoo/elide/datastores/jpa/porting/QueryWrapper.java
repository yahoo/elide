/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.porting;

import com.yahoo.elide.datastores.jpa.ScrollableIterator;
import com.yahoo.elide.datastores.jpql.porting.Query;

import lombok.Getter;

import java.util.Collection;
import java.util.Iterator;

/**
 * Wraps a JPA Query allowing most data store logic
 * to not directly depend on a specific version of JPA.
 */
public class QueryWrapper implements Query {
    @Getter
    private jakarta.persistence.Query query;

    public QueryWrapper(jakarta.persistence.Query query) {
        this.query = query;
    }

    @Override
    public Query setFirstResult(int num) {
        this.query = query.setFirstResult(num);
        return this;
    }

    @Override
    public Query setMaxResults(int num) {
        this.query = query.setMaxResults(num);
        return this;
    }

    @Override
    public Query setParameter(String name, Object value) {
        this.query = query.setParameter(name, value);
        return this;
    }

    @Override
    public Query setParameterList(String name, Collection values) {
        this.query = query.setParameter(name, values);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T uniqueResult() {
        return (T) query.getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> scroll() {
        Iterator<T> itr = query.getResultStream().iterator();
        return new ScrollableIterator<>(itr);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> list() {
        return query.getResultList();
    }
}
