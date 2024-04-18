/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jms;

import static com.paiondata.elide.graphql.subscriptions.SubscriptionModelBuilder.TOPIC_ARGUMENT;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.ArgumentType;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.graphql.subscriptions.annotations.Subscription;
import com.paiondata.elide.graphql.subscriptions.hooks.TopicType;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Elide datastore that reads models from JMS message topics.
 */
public class JMSDataStore implements DataStore {
    //Maps supported subscription models to whether or not they support topics.
    protected Map<Type<?>, Boolean> models;

    protected ConnectionFactory connectionFactory;
    protected EntityDictionary dictionary;

    protected long timeoutInMs = -1;

    //For serializing Elide models to topics.
    protected ObjectMapper objectMapper;

    /**
     * Constructor.
     * @param models The set of models to manage.
     * @param connectionFactory The JMS connection factory.
     * @param dictionary The entity dictionary.
     * @param objectMapper Object mapper for serializing/deserializing elide models to JMS topics.
     * @param timeout request timeout in milliseconds. 0 means immediate. null means no timeout.
     */
    public JMSDataStore(
            Set<Type<?>> models,
            ConnectionFactory connectionFactory,
            EntityDictionary dictionary,
            ObjectMapper objectMapper,
            Duration timeout
    ) {
        this.models = models.stream().collect(Collectors.toMap(
                model -> model,
                model -> {
                    Subscription subscription = model.getAnnotation(Subscription.class);
                    return subscription != null
                            && subscription.operations() != null
                            && subscription.operations().length > 0;
                }
        ));

        this.connectionFactory = connectionFactory;
        this.dictionary = dictionary;
        this.objectMapper = objectMapper;
        this.timeoutInMs = timeout != null ? timeout.toMillis() : -1;
    }

    /**
     * Constructor.
     * @param scanner to scan for subscription annotations.
     * @param connectionFactory The JMS connection factory.
     * @param dictionary The entity dictionary.
     * @param objectMapper Object mapper for serializing/deserializing elide models to JMS topics.
     * @param timeout request timeout in milliseconds. 0 means immediate. null means no timeout.
     */
    public JMSDataStore(
            ClassScanner scanner,
            ConnectionFactory connectionFactory,
            EntityDictionary dictionary,
            ObjectMapper objectMapper,
            Duration timeout
    ) {
        this(
                scanner.getAnnotatedClasses(Subscription.class, Include.class).stream()
                        .map(ClassType::of)
                        .collect(Collectors.toSet()),
                connectionFactory, dictionary, objectMapper, timeout);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Type<?> model : models.keySet()) {

            Boolean supportsTopics = models.get(model);

            dictionary.bindEntity(model);

            if (Boolean.TRUE.equals(supportsTopics)) {
                //Add topic type argument to each model.
                dictionary.addArgumentToEntity(model, ArgumentType
                        .builder()
                        .name(TOPIC_ARGUMENT)
                        .defaultValue(TopicType.ADDED)
                        .type(ClassType.of(TopicType.class))
                        .build());
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        JMSContext context = connectionFactory.createContext();
        return new JMSDataStoreTransaction(context, dictionary, objectMapper, timeoutInMs);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return beginTransaction();
    }
}
