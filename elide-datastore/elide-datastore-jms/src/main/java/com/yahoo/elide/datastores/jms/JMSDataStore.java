/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import static com.yahoo.elide.graphql.subscriptions.SubscriptionModelBuilder.TOPIC_ARGUMENT;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.ArgumentType;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.yahoo.elide.graphql.subscriptions.hooks.TopicType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;

import java.util.Set;
import java.util.stream.Collectors;
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

    /**
     * Constructor.
     * @param scanner to scan for subscription annotations.
     * @param connectionFactory The JMS connection factory.
     * @param dictionary The entity dictionary.
     * @param mapper Object mapper for serializing/deserializing elide models to JMS topics.
     * @param timeoutInMs request timeout in milliseconds.  0 means immediate.  -1 means no timeout.
     */
    public JMSDataStore(
            ClassScanner scanner,
            ConnectionFactory connectionFactory,
            EntityDictionary dictionary,
            ObjectMapper mapper,
            long timeoutInMs
    ) {
        this.models = scanner.getAnnotatedClasses(Subscription.class, SubscriptionField.class).stream()
                .map(ClassType::of)
                .collect(Collectors.toSet());

        this.connectionFactory = connectionFactory;
        this.dictionary = dictionary;
        this.mapper = mapper;
        this.timeoutInMs = timeoutInMs;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Type<?> model : models) {
            dictionary.bindEntity(model);

            //Add topic type argument to each model.
            dictionary.addArgumentToEntity(model, ArgumentType
                    .builder()
                    .name(TOPIC_ARGUMENT)
                    .type(ClassType.of(TopicType.class))
                    .build());
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
