/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.type.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Function;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

/**
 * Converts JMS messages to elide model instances via Jackson.
 * @param <T> elide model type.
 */
public class MessageDeserializer<T> implements Function<Message, T> {

    private ObjectMapper mapper;
    Type<?> type;

    /**
     * Constructor.
     * @param type The type to deserialize to.
     */
    public MessageDeserializer(Type<?> type) {
        this.type = type;
        this.mapper = new ObjectMapper();
    }

    @Override
    public T apply(Message message) {
        try {
            return (T) mapper.readValue(((TextMessage) message).getText(), type.getUnderlyingClass().get());
        } catch (JsonProcessingException | JMSException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
