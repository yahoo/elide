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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;

import java.util.Set;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;

/**
 * Elide datastore that reads models from JMS message topics.
 */
@Builder
public class JMSDataStore implements DataStore {
    protected Set<Type<?>> models;
    protected ConnectionFactory connectionFactory;
    protected EntityDictionary dictionary;
    protected ObjectMapper mapper;

    @Builder.Default
    protected long timeoutInMs = -1;

    /**
     * Constructor.
     * @param models The set of models to manage.
     * @param connectionFactory The JMS connection factory.
     * @param dictionary The entity dictionary.
     * @param mapper Object mapper for serializing/deserializing elide models to JMS topics.
     * @param timeoutInMs request timeout in milliseconds.  0 means immediate.  -1 means no timeout.
     */
    public JMSDataStore(
            Set<Type<?>> models,
            ConnectionFactory connectionFactory,
            EntityDictionary dictionary,
            ObjectMapper mapper,
            long timeoutInMs
    ) {
        this.models = models;
        this.connectionFactory = connectionFactory;
        this.dictionary = dictionary;
        this.mapper = mapper;
        this.timeoutInMs = timeoutInMs;
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
        return new JMSDataStoreTransaction(context, dictionary, timeoutInMs);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        JMSContext context = connectionFactory.createContext();
        return new JMSDataStoreTransaction(context, dictionary, timeoutInMs);
    }
}
