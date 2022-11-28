/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.jpa.transaction.JpaTransaction;
import com.yahoo.elide.datastores.jpql.JPQLDataStore;
import com.yahoo.elide.datastores.jpql.porting.QueryLogger;
import com.yahoo.elide.datastores.jpql.query.DefaultQueryLogger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation for JPA EntityManager data store.
 */
@Slf4j
public class JpaDataStore implements JPQLDataStore {
    public static final QueryLogger DEFAULT_LOGGER = new DefaultQueryLogger();

    protected final EntityManagerSupplier entityManagerSupplier;
    protected final JpaTransactionSupplier readTransactionSupplier;
    protected final JpaTransactionSupplier writeTransactionSupplier;
    protected final Set<Type<?>> modelsToBind;
    protected final QueryLogger logger;

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier readTransactionSupplier,
                        JpaTransactionSupplier writeTransactionSupplier,
                        QueryLogger logger,
                        Type<?> ... models) {
        this.entityManagerSupplier = entityManagerSupplier;
        this.readTransactionSupplier = readTransactionSupplier;
        this.writeTransactionSupplier = writeTransactionSupplier;
        this.logger = logger;
        this.modelsToBind = new HashSet<>();
        for (Type<?> model : models) {
            modelsToBind.add(model);
        }
    }

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier readTransactionSupplier,
                        JpaTransactionSupplier writeTransactionSupplier,
                        Type<?> ... models) {
        this(entityManagerSupplier, readTransactionSupplier, writeTransactionSupplier, DEFAULT_LOGGER, models);
    }


    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier transactionSupplier,
                        Type<?> ... models) {
        this(entityManagerSupplier, transactionSupplier, transactionSupplier, models);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        // If the user provided models, we'll manually add them and skip scanning for entities.
        if (! modelsToBind.isEmpty()) {
            modelsToBind.forEach((model) -> bindEntityClass(model, dictionary));
            return;
        }

        // Use the entities defined in the entity manager factory.
        for (EntityType type : entityManagerSupplier.get().getMetamodel().getEntities()) {
            try {
                Type<?> mappedClass = ClassType.of(type.getJavaType());
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
