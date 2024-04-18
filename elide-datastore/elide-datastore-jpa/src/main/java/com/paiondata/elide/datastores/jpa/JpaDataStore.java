/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpa;

import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.jpa.transaction.JpaTransaction;
import com.paiondata.elide.datastores.jpql.JPQLDataStore;
import com.paiondata.elide.datastores.jpql.porting.QueryLogger;
import com.paiondata.elide.datastores.jpql.query.DefaultQueryLogger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
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
    protected final MetamodelSupplier metamodelSupplier;
    protected final Set<Type<?>> modelsToBind;
    protected final QueryLogger queryLogger;

    private JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier readTransactionSupplier,
                        JpaTransactionSupplier writeTransactionSupplier,
                        QueryLogger queryLogger,
                        MetamodelSupplier metamodelSupplier,
                        Type<?>[] models) {
        this.entityManagerSupplier = entityManagerSupplier;
        this.readTransactionSupplier = readTransactionSupplier;
        this.writeTransactionSupplier = writeTransactionSupplier;
        this.metamodelSupplier = metamodelSupplier;
        this.queryLogger = queryLogger;
        this.modelsToBind = new HashSet<>();
        if (models != null) {
            Collections.addAll(this.modelsToBind, models);
        }
        if (this.metamodelSupplier == null && this.modelsToBind.isEmpty()) {
            throw new IllegalArgumentException(
                    "Either the metamodel supplier or the explicit models to bind needs to be provided.");
        }
    }

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier readTransactionSupplier,
                        JpaTransactionSupplier writeTransactionSupplier,
                        QueryLogger queryLogger,
                        MetamodelSupplier metamodelSupplier) {
        this(entityManagerSupplier, readTransactionSupplier, writeTransactionSupplier, queryLogger,
                metamodelSupplier, null);
    }

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
            JpaTransactionSupplier readTransactionSupplier,
            JpaTransactionSupplier writeTransactionSupplier,
            QueryLogger queryLogger,
            Type<?> ... models) {
        this(entityManagerSupplier, readTransactionSupplier, writeTransactionSupplier, queryLogger, null, models);
    }

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier transactionSupplier,
                        MetamodelSupplier metamodelSupplier) {
        this(entityManagerSupplier, transactionSupplier, transactionSupplier, DEFAULT_LOGGER, metamodelSupplier);
    }

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
            JpaTransactionSupplier transactionSupplier,
            Type<?> ... models) {
        this(entityManagerSupplier, transactionSupplier, transactionSupplier, DEFAULT_LOGGER, models);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        // If the user provided models, we'll manually add them and skip scanning for entities.
        if (! modelsToBind.isEmpty()) {
            modelsToBind.forEach(model -> bindEntityClass(model, dictionary));
            return;
        }

        // Use the entities defined in the entity manager factory.
        for (EntityType<?> type : metamodelSupplier.get().getEntities()) {
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

    /**
     * Functional interface for describing a method to supply Metamodel.
     */
    @FunctionalInterface
    public interface MetamodelSupplier {
        Metamodel get();
    }
}
