/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpql.porting;

import java.util.Collection;

/**
 * Interface that represents a Hibernate query but has no dependencies on a specific version of Hibernate.
 */
public interface Query {
    public Query setFirstResult(int num);
    public Query setMaxResults(int num);
    public Query setParameter(String name, Object value);
    public Query setParameterList(String name, Collection<?> values);
    public <T> T uniqueResult();
    public <T> Iterable<T> scroll();
    public <T> Iterable<T> list();
}
