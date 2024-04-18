/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jms;

import static com.paiondata.elide.graphql.subscriptions.SubscriptionModelBuilder.TOPIC_ARGUMENT;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreIterable;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.graphql.subscriptions.hooks.TopicType;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Data store transaction for reading Elide models from JMS topics.
 */
@Slf4j
public class JMSDataStoreTransaction implements DataStoreTransaction {
    private JMSContext context;
    private EntityDictionary dictionary;
    private ObjectMapper objectMapper;
    private long timeoutInMs;
    private List<JMSConsumer> consumers;

    /**
     * Constructor.
     * @param context JMS Context
     * @param dictionary Elide Entity Dictionary
     * @param objectMapper serializer to convert Elide models to topic messages.
     * @param timeoutInMs request timeout in milliseconds.  0 means immediate.  -1 means no timeout.
     */
    public JMSDataStoreTransaction(JMSContext context, EntityDictionary dictionary, ObjectMapper objectMapper,
            long timeoutInMs) {
        this.context = context;
        this.dictionary = dictionary;
        this.objectMapper = objectMapper;
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
                new MessageDeserializer<>(entityProjection.getType(), objectMapper)
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
