/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import lombok.Getter;

/**
 * JMS Topic Names.
 */
@Getter
public enum TopicType {
    ADDED("Added"),
    DELETED("Deleted"),
    UPDATED("Updated");

    private String topicSuffix;

    /**
     * Constructor.
     * @param topicSuffix The suffix of the topic name.
     */
    TopicType(String topicSuffix) {
        this.topicSuffix = topicSuffix;
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
}
