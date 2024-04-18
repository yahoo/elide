/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.hooks;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.graphql.subscriptions.annotations.Subscription;
import com.paiondata.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.paiondata.elide.graphql.subscriptions.serialization.GraphQLSubscriptionModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import lombok.Builder;

import java.util.function.Function;

/**
 * Scans for subscription annotations and registers lifecycle hooks to update JMS topics.
 */
@Builder
public class SubscriptionScanner {
    private ConnectionFactory connectionFactory;
    private ObjectMapper objectMapper;
    private EntityDictionary entityDictionary;
    private ClassScanner scanner;

    @Builder.Default
    private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;
    @Builder.Default
    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    @Builder.Default
    private long deliveryDelay = Message.DEFAULT_DELIVERY_DELAY;
    @Builder.Default
    private int messagePriority = Message.DEFAULT_PRIORITY;

    public void bindLifecycleHooks() {

        ObjectMapper objectMapper = this.objectMapper.copy().registerModule(new GraphQLSubscriptionModule());

        Function<JMSContext, JMSProducer> producerFactory = (context) -> {
            JMSProducer producer = context.createProducer();
            producer.setTimeToLive(timeToLive);
            producer.setDeliveryMode(deliveryMode);
            producer.setDeliveryDelay(deliveryDelay);
            producer.setPriority(messagePriority);
            return producer;
        };

        scanner.getAnnotatedClasses(Subscription.class).forEach(modelType -> {
            Subscription subscription = modelType.getAnnotation(Subscription.class);
            Preconditions.checkNotNull(subscription);

            Subscription.Operation[] operations = subscription.operations();

            for (Subscription.Operation operation : operations) {
                switch (operation) {
                    case UPDATE: {
                        addUpdateHooks(ClassType.of(modelType), entityDictionary, producerFactory, objectMapper);
                        break;
                    }
                    case DELETE: {
                        entityDictionary.bindTrigger(
                                modelType,
                                LifeCycleHookBinding.Operation.DELETE,
                                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                                new NotifyTopicLifeCycleHook(connectionFactory, objectMapper, producerFactory),
                                false
                        );
                        break;
                    }
                    case CREATE: {
                        entityDictionary.bindTrigger(
                                modelType,
                                LifeCycleHookBinding.Operation.CREATE,
                                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                                new NotifyTopicLifeCycleHook(connectionFactory, objectMapper, producerFactory),
                                false
                        );
                        break;
                    }
                }
            }
        });
    }

    protected void addUpdateHooks(
            Type<?> model,
            EntityDictionary dictionary,
            Function<JMSContext, JMSProducer> producerFactory,
            ObjectMapper objectMapper
    ) {
        dictionary.getAllExposedFields(model).stream().forEach(fieldName -> {
            SubscriptionField subscriptionField =
                    dictionary.getAttributeOrRelationAnnotation(model, SubscriptionField.class, fieldName);

            if (subscriptionField != null) {
                dictionary.bindTrigger(
                        model,
                        fieldName,
                        LifeCycleHookBinding.Operation.UPDATE,
                        LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                        new NotifyTopicLifeCycleHook(connectionFactory, objectMapper, producerFactory)
                );
            }
        });
    }
}
