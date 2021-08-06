/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.EntityProjection;

import com.google.common.base.Preconditions;

import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;

import java.io.IOException;

/**
 * Data store transaction for reading Elide models from JMS topics.
 */
public class JMSDataStoreTransaction implements DataStoreTransaction {
    private JMSContext context;
    private EntityDictionary dictionary;

    public JMSDataStoreTransaction(JMSContext context, EntityDictionary dictionary) {
        this.context = context;
        this.dictionary = dictionary;
    }

    @Override
    public <T> void save(T entity, RequestScope scope) {
        throw new BadRequestException("Unsupported operation");
    }

    @Override
    public <T> void delete(T entity, RequestScope scope) {
        throw new BadRequestException("Unsupported operation");
    }

    @Override
    public void flush(RequestScope scope) {

    }

    @Override
    public void commit(RequestScope scope) {

    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        throw new BadRequestException("Unsupported operation");
    }

    @Override
    public <T> Iterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        Preconditions.checkState(entityProjection.getArguments().size() == 1);

        Argument argument = entityProjection.getArguments().iterator().next();
        TopicType topicType = (TopicType) argument.getValue();

        String topicName = topicType.toTopicName(entityProjection.getType(), dictionary);

        Destination destination = context.createTopic(topicName);
        JMSConsumer consumer = context.createConsumer(destination);

        return new MessageIterator<>(
                consumer,
                ((SubscriptionRequestScope) scope).getTimeoutInMs(),
                new MessageDeserializer<>(entityProjection.getType())
        );
    }

    @Override
    public void cancel(RequestScope scope) {
        context.stop();
    }

    @Override
    public void close() throws IOException {
        context.stop();
        context.close();
    }
}
