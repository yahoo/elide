/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.Subscription;
import com.yahoo.elide.annotation.SubscriptionField;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.jms.hooks.NotifyTopicLifeCycleHook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

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
     * @param scanner Used to scan for subscription classes.
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
        this(
                scanner.getAnnotatedClasses(Subscription.class).stream()
                        .map(ClassType::of)
                        .collect(Collectors.toSet()),
                connectionFactory,
                dictionary,
                mapper,
                timeoutInMs
        );
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Type<?> model : models) {
            dictionary.bindEntity(model);
            Subscription subscription = model.getAnnotation(Subscription.class);
            Preconditions.checkNotNull(subscription);

            Subscription.Operation[] operations = subscription.operations();

            for (Subscription.Operation operation : operations) {
                switch (operation) {
                    case UPDATE: {
                        addUpdateHooks(model, dictionary);
                        break;
                    }
                    case DELETE: {
                        dictionary.bindTrigger(
                                model,
                                LifeCycleHookBinding.Operation.DELETE,
                                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                                new NotifyTopicLifeCycleHook(connectionFactory, mapper),
                                false
                        );
                        break;
                    }
                    case CREATE: {
                        dictionary.bindTrigger(
                                model,
                                LifeCycleHookBinding.Operation.CREATE,
                                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                                new NotifyTopicLifeCycleHook(connectionFactory, mapper),
                                false
                        );
                        break;
                    }
                }
            }
        }
    }

    protected void addUpdateHooks(Type<?> model, EntityDictionary dictionary) {
        dictionary.getAllFields(model).stream().forEach(fieldName -> {
            SubscriptionField subscriptionField =
                    dictionary.getAttributeOrRelationAnnotation(model, SubscriptionField.class, fieldName);

            if (subscriptionField != null) {
                dictionary.bindTrigger(
                        model,
                        fieldName,
                        LifeCycleHookBinding.Operation.UPDATE,
                        LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                        new NotifyTopicLifeCycleHook(connectionFactory, mapper)
                );
            }
        });
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
