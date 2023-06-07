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
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

        GsonBuilder gsonBuilder = new GsonBuilder();
        CoerceUtil.getSerdes().forEach((cls, serde) -> {
            gsonBuilder.registerTypeAdapter(cls, new SubscriptionFieldSerde(serde, cls));
        });
        gsonBuilder.addSerializationExclusionStrategy(new SubscriptionExclusionStrategy()).serializeNulls();

        Gson gson = gsonBuilder.create();

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
                        addUpdateHooks(ClassType.of(modelType), entityDictionary, producerFactory, gson);
                        break;
                    }
                    case DELETE: {
                        entityDictionary.bindTrigger(
                                modelType,
                                LifeCycleHookBinding.Operation.DELETE,
                                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                                new NotifyTopicLifeCycleHook(connectionFactory, producerFactory, gson),
                                false
                        );
                        break;
                    }
                    case CREATE: {
                        entityDictionary.bindTrigger(
                                modelType,
                                LifeCycleHookBinding.Operation.CREATE,
                                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                                new NotifyTopicLifeCycleHook(connectionFactory, producerFactory, gson),
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
            Gson gson
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
                        new NotifyTopicLifeCycleHook(connectionFactory, producerFactory, gson)
                );
            }
        });
    }
}
