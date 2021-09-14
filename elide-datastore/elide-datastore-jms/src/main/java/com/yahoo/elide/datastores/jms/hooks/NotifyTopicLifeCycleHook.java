/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.lifecycle.CRUDEvent;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.jms.TopicType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;

/**
 * Life cycle hook that sends serialized model events to a JMS topic.
 * This will be registered automatically by Elide for all models that have the Subscription annotation.
 *
 * @param <T> The model type.
 */
@Slf4j
@AllArgsConstructor //For testing
@NoArgsConstructor  //For injection
public class NotifyTopicLifeCycleHook<T> implements LifeCycleHook<T> {

    @Inject
    private ConnectionFactory connectionFactory;

    @Inject
    private ObjectMapper mapper;

    @Override
    public void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            CRUDEvent event) {

        JMSContext context = connectionFactory.createContext();

        PersistentResource<T> resource = (PersistentResource<T>) event.getResource();

        Type<?> modelType = resource.getResourceType();

        TopicType topicType = TopicType.fromOperation(operation);
        String topicName = topicType.toTopicName(modelType, resource.getDictionary());

        JMSProducer producer = context.createProducer();
        Destination destination = context.createTopic(topicName);

        try {
            producer.send(destination, mapper.writeValueAsString(resource.getObject()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
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
}
