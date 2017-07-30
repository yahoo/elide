/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;

import java.util.Collection;

public class TestSessionWrapper implements Session {
    @Override
    public Query createQuery(String queryText) {
        return new TestQueryWrapper(queryText);
    }

    @Override
    public Query createFilter(Collection collection, String queryText) {
        return new TestQueryWrapper(queryText);
    }
}
