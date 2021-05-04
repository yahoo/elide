/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.JPQLDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.executors.ActivePermissionExecutor;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.jpa.porting.QueryLogger;
import com.yahoo.elide.datastores.jpa.transaction.JpaTransaction;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

/**
 * Implementation for JPA EntityManager data store.
 */
@Slf4j
public class JpaDataStore implements JPQLDataStore {
    public static final QueryLogger DEFAULT_LOGGER = (query) -> log.debug("HQL Query: {}", query);

    protected final EntityManagerSupplier entityManagerSupplier;
    protected final JpaTransactionSupplier readTransactionSupplier;
    protected final JpaTransactionSupplier writeTransactionSupplier;
    protected final Function<RequestScope, PermissionExecutor> permissionExecutorFunction;
    protected final Set<Type<?>> modelsToBind;
    protected final QueryLogger logger;

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier readTransactionSupplier,
                        JpaTransactionSupplier writeTransactionSupplier,
                        Function<RequestScope, PermissionExecutor> permissionExecutorFunction,
                        QueryLogger logger,
                        Type<?> ... models) {
        this.entityManagerSupplier = entityManagerSupplier;
        this.readTransactionSupplier = readTransactionSupplier;
        this.writeTransactionSupplier = writeTransactionSupplier;
        this.permissionExecutorFunction = permissionExecutorFunction;
        this.logger = logger;
        this.modelsToBind = new HashSet<>();
        for (Type<?> model : models) {
            modelsToBind.add(model);
        }
    }

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier readTransactionSupplier,
                        JpaTransactionSupplier writeTransactionSupplier,
                        Function<RequestScope, PermissionExecutor> permissionExecutorFunction,
                        Type<?> ... models) {
        this(entityManagerSupplier, readTransactionSupplier, writeTransactionSupplier, permissionExecutorFunction,
                DEFAULT_LOGGER, models);
    }

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier transactionSupplier,
                        Function<RequestScope, PermissionExecutor> permissionExecutorFunction,
                        Type<?> ... models) {
        this(entityManagerSupplier, transactionSupplier, transactionSupplier, permissionExecutorFunction, models);
    }

    public JpaDataStore(EntityManagerSupplier entityManagerSupplier,
                        JpaTransactionSupplier transactionSupplier,
                        Type<?> ... models) {
        this(entityManagerSupplier, transactionSupplier, transactionSupplier, ActivePermissionExecutor::new, models);
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

    @Override
    public Function<RequestScope, PermissionExecutor> getPermissionExecutorFunction() {
        return permissionExecutorFunction;
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
