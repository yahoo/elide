/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3.porting;

import com.yahoo.elide.core.hibernate.Query;

import lombok.Getter;

import java.util.Collection;

/**
 * Wraps a Hibernate 3 Query allowing most data store logic
 * to not directly depend on a specific version of Hibernate.
 */
public class QueryWrapper implements Query {
    @Getter
    private org.hibernate.Query query;

    public QueryWrapper (org.hibernate.Query query) {
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
        this.query = query.setParameterList(name, values);
        return this;
    }
}
