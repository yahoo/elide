/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.jpa.transaction.JpaTransaction;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

/**
 * Implementation for JPA EntityManager data store.
 */
public class JpaDataStore implements DataStore {
    protected final EntityManagerSupplier entityManagerSupplier;
    protected final JpaTransactionSupplier transactionSupplier;

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier transactionSupplier) {
        this.entityManagerSupplier = entityManagerSupplier;
        this.transactionSupplier = transactionSupplier;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (EntityType type : entityManagerSupplier.get().getMetamodel().getEntities()) {
            try {
                Class<?> mappedClass = type.getJavaType();
                // Ignore this result. We are just checking to see if it throws an exception meaning that
                // provided class was _not_ an entity.
                dictionary.lookupEntityClass(mappedClass);

                // Bind if successful
                dictionary.bindEntity(mappedClass);
            } catch (IllegalArgumentException e) {
                // Ignore this entity.
                // Turns out that JPA may include non-entity types in this list when using things like envers.
                // Since they are not entities, we do not want to bind them into the entity dictionary.
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        EntityManager entityManager = entityManagerSupplier.get();
        JpaTransaction transaction = transactionSupplier.get(entityManager);
        transaction.begin();
        return transaction;
    }

    /**
     * Functional interface for describing a method to supply EntityManager.
     */
    @FunctionalInterface
    public interface EntityManagerSupplier {
        EntityManager get();
    }

    /**
     * Functional interface for describing a method to supply JpaTransaction.
     */
    @FunctionalInterface
    public interface JpaTransactionSupplier {
        JpaTransaction get(EntityManager entityManager);
    }
}
