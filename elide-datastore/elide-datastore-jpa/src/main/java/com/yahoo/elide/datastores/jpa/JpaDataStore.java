/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.JPQLDataStore;
import com.yahoo.elide.datastores.jpa.transaction.JpaTransaction;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

/**
 * Implementation for JPA EntityManager data store.
 */
public class JpaDataStore implements JPQLDataStore {
    protected final EntityManagerSupplier entityManagerSupplier;
    protected final JpaTransactionSupplier readTransactionSupplier;
    protected final JpaTransactionSupplier writeTransactionSupplier;
    protected final Set<Class<?>> modelsToBind;

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier readTransactionSupplier,
                        JpaTransactionSupplier writeTransactionSupplier,
                        Class<?> ... models) {
        this.entityManagerSupplier = entityManagerSupplier;
        this.readTransactionSupplier = readTransactionSupplier;
        this.writeTransactionSupplier = writeTransactionSupplier;
        this.modelsToBind = new HashSet<>();
        for (Class<?> model : models) {
            modelsToBind.add(model);
        }
    }


    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier transactionSupplier,
                        Class<?> ... models) {
        this(entityManagerSupplier, transactionSupplier, transactionSupplier, models);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        // If the user provided models, we'll manually add them and skip scanning for entities.
        if (! modelsToBind.isEmpty()) {
            modelsToBind.stream().forEach((model) -> bindEntityClass(model, dictionary));
            return;
        }

        // Use the entities defined in the entity manager factory.
        for (EntityType type : entityManagerSupplier.get().getMetamodel().getEntities()) {
            try {
                Class<?> mappedClass = type.getJavaType();
                // Ignore this result. We are just checking to see if it throws an exception meaning that
                // provided class was _not_ an entity.
                dictionary.lookupEntityClass(mappedClass);

                bindEntityClass(mappedClass, dictionary);
            } catch (IllegalArgumentException e) {
                // Ignore this entity.
                // Turns out that JPA may include non-entity types in this list when using things like envers.
                // Since they are not entities, we do not want to bind them into the entity dictionary.
            }
        }
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        EntityManager entityManager = entityManagerSupplier.get();
        JpaTransaction transaction = readTransactionSupplier.get(entityManager);
        transaction.begin();
        return transaction;
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        EntityManager entityManager = entityManagerSupplier.get();
        JpaTransaction transaction = writeTransactionSupplier.get(entityManager);
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
