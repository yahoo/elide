/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.porting;

/**
 * Interface that represents a Hibernate session but has no dependencies on a specific version of Hibernate.
 */
public interface Session {
    public Query createQuery(String queryText);
}
