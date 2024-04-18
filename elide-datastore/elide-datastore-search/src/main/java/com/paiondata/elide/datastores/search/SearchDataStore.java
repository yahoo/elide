/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.search;

import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.google.common.base.Preconditions;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import jakarta.persistence.EntityManagerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

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
            SearchSession session = Search.session(entityManagerFactory.createEntityManager());
            try {
                Set<Class<?>> classesToIndex = new LinkedHashSet<>();
                for (Type<?> entityType : entityDictionary.getBoundClasses()) {
                    if (entityDictionary.getAnnotation(entityType, Indexed.class) != null) {
                        Preconditions.checkState(entityType instanceof ClassType);
                        Class<?> entityClass = ((ClassType) entityType).getCls();
                        classesToIndex.add(entityClass);
                    }
                }
                MassIndexer indexer = session.massIndexer(classesToIndex);
                indexer.startAndWait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                session.toOrmSession().close();
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

        SearchSession session = Search.session(entityManagerFactory.createEntityManager());
        return new SearchDataTransaction(wrapped.beginReadTransaction(), dictionary,
                session, minNgramSize, maxNgramSize);
    }
}
