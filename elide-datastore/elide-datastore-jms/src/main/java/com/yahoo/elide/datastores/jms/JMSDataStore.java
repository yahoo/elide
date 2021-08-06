/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;

import java.util.Set;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;

/**
 * Elide datastore that reads models from JMS message topics.
 */
public class JMSDataStore implements DataStore {
    private Set<Type<?>> models;
    private ConnectionFactory connectionFactory;
    private EntityDictionary dictionary;

    /**
     * Constructor.
     * @param models The set of models to manage.
     * @param connectionFactory The JMS connection factory.
     * @param dictionary The entity dictionary.
     */
    public JMSDataStore(
            Set<Type<?>> models,
            ConnectionFactory connectionFactory,
            EntityDictionary dictionary) {
        this.models = models;
        this.connectionFactory = connectionFactory;
        this.dictionary = dictionary;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Type<?> model : models) {
            dictionary.bindEntity(model);
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        JMSContext context = connectionFactory.createContext();
        return new JMSDataStoreTransaction(context, dictionary);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        JMSContext context = connectionFactory.createContext();
        return new JMSDataStoreTransaction(context, dictionary);
    }
}
