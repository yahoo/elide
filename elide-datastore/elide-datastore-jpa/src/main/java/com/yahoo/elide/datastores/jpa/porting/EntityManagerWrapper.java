/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.porting;

import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;

import com.yahoo.elide.datastores.jpql.porting.Query;
import com.yahoo.elide.datastores.jpql.porting.QueryLogger;
import com.yahoo.elide.datastores.jpql.porting.Session;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps a JPA EntityManager allowing most data store logic
 * to not directly depend on a specific version of JPA.
 */
@Slf4j
public class EntityManagerWrapper implements Session {
    private EntityManager entityManager;
    private QueryLogger logger;

    public EntityManagerWrapper(EntityManager entityManager) {
        this(entityManager, DEFAULT_LOGGER);
    }

    public EntityManagerWrapper(EntityManager entityManager, QueryLogger logger) {
        this.entityManager = entityManager;
        this.logger = logger;
    }

    @Override
    public Query createQuery(String queryText) {
        Query query = new QueryWrapper(entityManager.createQuery(queryText));
        logger.log(String.format("Query Hash: %d\tHQL Query: %s", query.hashCode(), queryText));
        return query;
    }
}
