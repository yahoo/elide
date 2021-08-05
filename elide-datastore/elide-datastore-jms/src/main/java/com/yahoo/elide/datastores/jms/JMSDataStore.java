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
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

public class JMSDataStore implements DataStore {
    Set<Type<?>> models;
    ConnectionFactory connectionFactory;

    public JMSDataStore(Set<Type<?>> models, ConnectionFactory connectionFactory) {
        this.models = models;
        this.connectionFactory = connectionFactory;
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
        try {
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession();
            Destination destination = session.createTopic("foo");
            MessageConsumer consumer = session.createConsumer(destination);



        } catch (JMSException e) {

        }
        return new JMSDataStoreTransaction(context);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        JMSContext context = connectionFactory.createContext();
        return new JMSDataStoreTransaction(context);
    }
}
