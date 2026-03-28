/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.hooks;

import com.yahoo.elide.ElideMapper;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.lifecycle.CRUDEvent;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.type.Type;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;

import java.util.Optional;
import java.util.function.Function;

/**
 * Life cycle hook that sends serialized model events to a JMS topic.
 * This will be registered automatically by Elide for all models that have the Subscription annotation.
 *
 * @param <T> The model type.
 */
@Slf4j
@NoArgsConstructor  //For injection
public class NotifyTopicLifeCycleHook<T> implements LifeCycleHook<T> {

    @Inject
    private ConnectionFactory connectionFactory;

    @Inject
    private ElideMapper elideMapper;

    @Inject
    private Function<JMSContext, JMSProducer> createProducer;

    public NotifyTopicLifeCycleHook(
            ConnectionFactory connectionFactory,
            ElideMapper elideMapper,
            Function<JMSContext, JMSProducer> createProducer
    ) {
        this.connectionFactory = connectionFactory;
        this.createProducer = createProducer;
        this.elideMapper = elideMapper;
    }

    @Override
    public void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            CRUDEvent event) {

        PersistentResource<T> resource = (PersistentResource<T>) event.getResource();

        Type<?> modelType = resource.getResourceType();
        TopicType topicType = TopicType.fromOperation(operation);
        String topicName = topicType.toTopicName(modelType, resource.getDictionary());

        publish(resource.getObject(), topicName);
    }

    @Override
    public void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            T elideEntity,
            RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        //NOOP
    }

    /**
     * Publishes an object to a JMS topic.
     * @param object The object to publish.
     * @param topicName The topic name to publish to.
     */
    public void publish(T object, String topicName) {
        try (JMSContext context = connectionFactory.createContext()) {

            JMSProducer producer = createProducer.apply(context);
            Destination destination = context.createTopic(topicName);

            try {
                String message = elideMapper.getObjectMapper().writeValueAsString(object);
                log.debug("Serializing {}", message);
                producer.send(destination, message);
            } catch (JacksonException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
