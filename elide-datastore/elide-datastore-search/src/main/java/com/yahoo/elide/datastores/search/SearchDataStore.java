/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.search.constraints.SearchConstraint;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;

/**
 * Performs full text search when it can.  Otherwise delegates to a wrapped store.
 */
public class SearchDataStore implements DataStore {

    private DataStore wrapped;
    private EntityDictionary dictionary;
    private EntityManagerFactory entityManagerFactory;
    private boolean indexOnStartup = false;

    private static Map<Class<?>, List<SearchConstraint>> searchConstraintsOverrides = new HashMap<>();

    public SearchDataStore(DataStore wrapped, EntityManagerFactory entityManagerFactory, boolean indexOnStartup) {
        this.wrapped = wrapped;
        this.entityManagerFactory = entityManagerFactory;
        this.indexOnStartup = indexOnStartup;

    }

    @Override
    public void populateEntityDictionary(EntityDictionary entityDictionary) {
        wrapped.populateEntityDictionary(entityDictionary);

        if (indexOnStartup) {

            FullTextEntityManager em = Search.getFullTextEntityManager(entityManagerFactory.createEntityManager());
            try {
                for (Class<?> entityClass : entityDictionary.getBindings()) {
                    if (entityDictionary.getAnnotation(entityClass, Indexed.class) != null) {
                        em.createIndexer(entityClass).startAndWait();
                    }
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            } finally {
                em.close();
            }
        }

        this.dictionary = entityDictionary;
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return wrapped.beginTransaction();
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {

        FullTextEntityManager em = Search.getFullTextEntityManager(entityManagerFactory.createEntityManager());

        return new SearchDataTransaction(wrapped.beginReadTransaction(), dictionary, em);
    }

    /**
     * Depending on how an entity is indexed, it is possible to override the constraints that determine
     * how a given filter predicate is handled by the store.  The behaviors include:
     * 1. Use the search data store fully for filtering.
     * 2. Use the search data store partially for filtering (some will be performed in memory)
     * 2. Delegate to the underlying wrapped store
     * 3. Throw a HTTPStatusException
     * @param entityClass The entity class to override the behavior for.
     * @param constraints The list of constraints to apply for that class.
     */
    public static void registerSearchConstraints(Class<?> entityClass, List<SearchConstraint> constraints) {
        searchConstraintsOverrides.put(entityClass, constraints);
    }


    /**
     * Looks up the registered constraint overrides for the given entity class.
     * @param entityClass The class to lookup
     * @param defaultConstraints If there are no overrides defined for the given class, this parameter is returned.
     * @return The overridden constraints or defaultConstraints if no overrides have been defined.
     */
    public static List<SearchConstraint> lookupSearchConstraints(Class<?> entityClass,
                                                                 List<SearchConstraint> defaultConstraints) {
        return searchConstraintsOverrides.getOrDefault(entityClass, defaultConstraints);
    }
}
