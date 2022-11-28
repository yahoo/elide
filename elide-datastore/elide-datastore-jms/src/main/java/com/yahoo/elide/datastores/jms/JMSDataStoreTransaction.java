/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import static com.yahoo.elide.graphql.subscriptions.SubscriptionModelBuilder.TOPIC_ARGUMENT;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.graphql.subscriptions.hooks.TopicType;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSRuntimeException;

/**
 * Data store transaction for reading Elide models from JMS topics.
 */
@Slf4j
public class JMSDataStoreTransaction implements DataStoreTransaction {
    private JMSContext context;
    private EntityDictionary dictionary;
    private Gson gson;
    private long timeoutInMs;
    private List<JMSConsumer> consumers;

    /**
     * Constructor.
     * @param context JMS Context
     * @param dictionary Elide Entity Dictionary
     * @param gson Gson serializer to convert Elide models to topic messages.
     * @param timeoutInMs request timeout in milliseconds.  0 means immediate.  -1 means no timeout.
     */
    public JMSDataStoreTransaction(JMSContext context, EntityDictionary dictionary, Gson gson, long timeoutInMs) {
        this.context = context;
        this.gson = gson;
        this.dictionary = dictionary;
        this.timeoutInMs = timeoutInMs;
        this.consumers = new ArrayList<>();
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
    public <T> DataStoreIterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        TopicType topicType = getTopicType(entityProjection);

        String topicName = topicType.toTopicName(entityProjection.getType(), dictionary);

        Destination destination = context.createTopic(topicName);
        JMSConsumer consumer = context.createConsumer(destination);

        context.start();

        consumers.add(consumer);

        return new MessageIterable<>(
                consumer,
                timeoutInMs,
                new MessageDeserializer<>(entityProjection.getType(), gson)
        );
    }

    @Override
    public void cancel(RequestScope scope) {
        shutdown();
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }

    private void shutdown() {
        try {
            consumers.forEach(JMSConsumer::close);
            context.stop();
            context.close();
        } catch (JMSRuntimeException e) {
            log.debug("Exception throws while closing context: {}", e.getMessage());
        }
    }

    protected TopicType getTopicType(EntityProjection projection) {
        Set<Argument> arguments = projection.getArguments();

        for (Argument argument: arguments) {
            if (argument.getName().equals(TOPIC_ARGUMENT)) {
                return (TopicType) argument.getValue();
            }
        }

        return TopicType.CUSTOM;
    }
}
