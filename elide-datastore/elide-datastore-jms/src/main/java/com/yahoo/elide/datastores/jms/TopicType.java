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

    TopicType(String topicSuffix) {
        this.topicSuffix = topicSuffix;
    }

    public String toTopicName(Type<?> type, EntityDictionary dictionary) {
        return dictionary.getJsonAliasFor(type) + topicSuffix;
    }
}
