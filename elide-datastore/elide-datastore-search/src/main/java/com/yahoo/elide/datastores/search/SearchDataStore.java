/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.google.common.base.Preconditions;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

import jakarta.persistence.EntityManagerFactory;

/**
 * Performs full text search when it can.  Otherwise delegates to a wrapped store.
 */
public class SearchDataStore implements DataStore {

    private static final int DEFAULT_MIN_NGRAM = 3;
    private static final int DEFAULT_MAX_NGRAM = 5;

    private DataStore wrapped;
    private EntityDictionary dictionary;
    private EntityManagerFactory entityManagerFactory;
    private boolean indexOnStartup = false;
    private int minNgramSize;
    private int maxNgramSize;

    public SearchDataStore(DataStore wrapped, EntityManagerFactory entityManagerFactory, boolean indexOnStartup) {
        this(wrapped, entityManagerFactory, indexOnStartup, DEFAULT_MIN_NGRAM, DEFAULT_MAX_NGRAM);
    }

    public SearchDataStore(DataStore wrapped,
                           EntityManagerFactory entityManagerFactory,
                           boolean indexOnStartup,
                           int minNgramSize,
                           int maxNgramSize) {
        this.wrapped = wrapped;
        this.entityManagerFactory = entityManagerFactory;
        this.indexOnStartup = indexOnStartup;

        this.minNgramSize = minNgramSize;
        this.maxNgramSize = maxNgramSize;
    }


    @Override
    public void populateEntityDictionary(EntityDictionary entityDictionary) {
        wrapped.populateEntityDictionary(entityDictionary);

        if (indexOnStartup) {

            FullTextEntityManager em = Search.getFullTextEntityManager(entityManagerFactory.createEntityManager());
            try {
                for (Type<?> entityType : entityDictionary.getBoundClasses()) {
                    if (entityDictionary.getAnnotation(entityType, Indexed.class) != null) {
                        Preconditions.checkState(entityType instanceof ClassType);
                        Class<?> entityClass = ((ClassType) entityType).getCls();
                        em.createIndexer(entityClass).startAndWait();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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

        return new SearchDataTransaction(wrapped.beginReadTransaction(), dictionary, em, minNgramSize, maxNgramSize);
    }
}
