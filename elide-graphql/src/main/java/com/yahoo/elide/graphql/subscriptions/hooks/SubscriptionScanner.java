/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;

import javax.jms.ConnectionFactory;

/**
 * Scans for subscription annotations and registers lifecycle hooks to update JMS topics.
 */
@AllArgsConstructor
public class SubscriptionScanner {
    private ConnectionFactory connectionFactory;
    private ObjectMapper mapper;
    private EntityDictionary dictionary;
    private ClassScanner scanner;

    public void bindLifecycleHooks() {
        scanner.getAnnotatedClasses(Subscription.class).forEach(modelType -> {
            Subscription subscription = modelType.getAnnotation(Subscription.class);
            Preconditions.checkNotNull(subscription);

            Subscription.Operation[] operations = subscription.operations();

            for (Subscription.Operation operation : operations) {
                switch (operation) {
                    case UPDATE: {
                        addUpdateHooks(ClassType.of(modelType), dictionary);
                        break;
                    }
                    case DELETE: {
                        dictionary.bindTrigger(
                                modelType,
                                LifeCycleHookBinding.Operation.DELETE,
                                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                                new NotifyTopicLifeCycleHook(connectionFactory, mapper),
                                false
                        );
                        break;
                    }
                    case CREATE: {
                        dictionary.bindTrigger(
                                modelType,
                                LifeCycleHookBinding.Operation.CREATE,
                                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                                new NotifyTopicLifeCycleHook(connectionFactory, mapper),
                                false
                        );
                        break;
                    }
                }
            }
        });
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
}
