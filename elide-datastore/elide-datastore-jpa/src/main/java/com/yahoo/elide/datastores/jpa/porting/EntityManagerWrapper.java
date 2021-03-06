/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.porting;

import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.EntityManager;

/**
 * Wraps a JPA EntityManager allowing most data store logic
 * to not directly depend on a specific version of JPA.
 */
@Slf4j
public class EntityManagerWrapper implements Session {
    private EntityManager entityManager;
    private QueryLogger logger;

    public EntityManagerWrapper(EntityManager entityManager) {
        this(entityManager, (queryText) -> log.debug("HQL Query: {}", queryText));
    }

    public EntityManagerWrapper(EntityManager entityManager, QueryLogger logger) {
        this.entityManager = entityManager;
        this.logger = logger;
    }

    @Override
    public Query createQuery(String queryText) {
        logger.log(queryText);
        return new QueryWrapper(entityManager.createQuery(queryText));
    }
}
