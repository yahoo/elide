/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.porting;

import com.yahoo.elide.core.hibernate.Query;

import lombok.Getter;

import java.util.Collection;

/**
 * Wraps a JPA Query allowing most data store logic
 * to not directly depend on a specific version of JPA.
 */
public class QueryWrapper implements Query {
    @Getter
    private javax.persistence.Query query;

    public QueryWrapper(javax.persistence.Query query) {
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
}
