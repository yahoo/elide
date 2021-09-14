/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.ResourceLineage;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.lifecycle.CRUDEvent;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.jms.TopicType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;

/**
 * Life cycle hook that sends serialized model events to a JMS topic.
 * @param <T> The model type.
 */
@Slf4j
public class NotifyTopicLifeCycleHook<T> implements LifeCycleHook<T> {

    @Inject
    private JMSContext context;

    @Inject
    @Named("subscriptionModels")
    private Set<Type<?>> modelsWithTopics;

    @Inject
    private ObjectMapper mapper;

    @Override
    public void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            CRUDEvent event) {

        PersistentResource<T> resource = (PersistentResource<T>) event.getResource();

        //We only have topics for models managed by this store.  If an ancestor model
        //triggered the hook, we need to locate the managed model through its lineage.
        Type<?> modelType = findManagedModel(resource);

        //Ignore the lifecycle change if the model is not managed.
        if (modelType == null) {
            log.debug("Ignoring life cycle event {} {} {}", operation, phase, event);
            return;
        }

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

    private Type<?> findManagedModel(PersistentResource<T> resource) {
        EntityDictionary dictionary = resource.getDictionary();
        Type<?> modelType = resource.getResourceType();

        if (modelsWithTopics.contains(modelType)) {
            return modelType;
        }

        //If the type is not 'owned' and doesn't have a topic, it isn't managed.
        if (! dictionary.isTransferable(modelType)) {
            return null;
        }

        //Invert the resource lineage to work backwards...
        List<ResourceLineage.LineagePath> inversePath = new ArrayList<>();
        inversePath.addAll(resource.getLineage().getResourcePath());
        Collections.reverse(inversePath);

        for (ResourceLineage.LineagePath pathElement : inversePath) {
            modelType = pathElement.getResource().getResourceType();
            if (modelsWithTopics.contains(modelType)) {
                return modelType;
            } else if (! dictionary.isTransferable(modelType)) {
                return null;
            }
        }

        return null;
    }
}
