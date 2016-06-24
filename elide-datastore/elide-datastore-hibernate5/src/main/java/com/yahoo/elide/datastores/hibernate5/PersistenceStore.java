/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

import com.google.common.base.Preconditions;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;

/**
 * Manager for javax.persistence compatible db resource.
 */
public class PersistenceStore implements DataStore {
    private final EntityManagerFactory entityManagerFactory;

    public PersistenceStore(EntityManagerFactory entityManagerFactory) {
        Preconditions.checkNotNull(entityManagerFactory);
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (EntityType entity : entityManagerFactory.getMetamodel().getEntities()) {
            dictionary.bindEntity(entity.getBindableJavaType());
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new PersistenceTransaction(entityManagerFactory.createEntityManager());
    }
}
