/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.hibernate.hql;

import com.paiondata.elide.datastores.jpql.porting.Query;
import com.paiondata.elide.datastores.jpql.porting.Session;

public class TestSessionWrapper implements Session {
    @Override
    public Query createQuery(String queryText) {
        return new TestQueryWrapper(queryText);
    }

    @Override
    public <T> T find(String queryText, Class<T> entityClass, Object primaryKey) {
        return null;
    }
}
