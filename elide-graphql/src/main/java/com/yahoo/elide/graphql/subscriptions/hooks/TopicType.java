/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.hooks;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;

import lombok.Getter;

/**
 * JMS Topic Names.
 */
@Getter
public enum TopicType {
    ADDED("Added", CREATE),
    DELETED("Deleted", DELETE),
    UPDATED("Updated", UPDATE),
    CUSTOM("", null);

    private final String topicSuffix;
    private final LifeCycleHookBinding.Operation operation;

    /**
     * Constructor.
     * @param topicSuffix The suffix of the topic name.
     */
    TopicType(String topicSuffix, LifeCycleHookBinding.Operation operation) {
        this.topicSuffix = topicSuffix;
        this.operation = operation;
    }

    /**
     * Converts a TopicType to a JMS topic name.
     * @param type Elide model type.
     * @param dictionary Elide entity dictionary.
     * @return a JMS topic name.
     */
    public String toTopicName(Type<?> type, EntityDictionary dictionary) {
        return dictionary.getJsonAliasFor(type) + topicSuffix;
    }

    /**
     * Converts a LifeCycleHookBinding to a topic type.
     * @param op The lifecyle operation
     * @return The corresponding topic type.
     */
    public static TopicType fromOperation(LifeCycleHookBinding.Operation op) {
        switch (op) {
            case CREATE: {
                return ADDED;
            }
            case DELETE: {
                return DELETED;
            }
            default : {
                return UPDATED;
            }
        }
    }

    /**
     * Converts a LifeCycleHookBinding to a topic type.
     * @param op The lifecyle operation
     * @return The corresponding topic type.
     */
    public static TopicType fromOperation(Subscription.Operation op) {
        switch (op) {
            case CREATE: {
                return ADDED;
            }
            case DELETE: {
                return DELETED;
            }
            default : {
                return UPDATED;
            }
        }
    }
}
