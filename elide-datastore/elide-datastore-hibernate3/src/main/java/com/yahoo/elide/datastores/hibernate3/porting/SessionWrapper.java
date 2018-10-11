/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3.porting;

import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;

import lombok.extern.slf4j.Slf4j;

/**
 * Wraps a Hibernate 3 Session allowing most data store logic
 * to not directly depend on a specific version of Hibernate.
 */
@Slf4j
public class SessionWrapper implements Session {

    private org.hibernate.Session session;

    public SessionWrapper(org.hibernate.Session session) {
        this.session = session;
    }

    @Override
    public Query createQuery(String queryText) {
        logQuery(queryText);
        return new QueryWrapper(session.createQuery(queryText));
    }

    private static void logQuery(String queryText) {
        log.debug("HQL Query: {}", queryText);
    }
}
