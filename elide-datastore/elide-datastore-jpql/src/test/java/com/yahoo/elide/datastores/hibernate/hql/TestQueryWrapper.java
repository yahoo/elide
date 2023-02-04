/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import com.yahoo.elide.datastores.jpql.porting.Query;

import lombok.Getter;

import java.util.Collection;

public class TestQueryWrapper implements Query {

    @Getter
    private String queryText;

    public TestQueryWrapper(String queryText) {
        this.queryText = queryText;
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

    @Override
    public <T> T uniqueResult() {
        return null;
    }

    @Override
    public <T> Iterable<T> scroll() {
        return null;
    }

    @Override
    public <T> Iterable<T> list() {
        return null;
    }
}
