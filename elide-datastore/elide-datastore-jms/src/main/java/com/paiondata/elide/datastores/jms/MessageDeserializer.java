/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jms;

import com.paiondata.elide.core.exceptions.InternalServerErrorException;
import com.paiondata.elide.core.type.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import java.util.function.Function;

/**
 * Converts JMS messages to elide model instances via Jackson.
 * @param <T> elide model type.
 */
public class MessageDeserializer<T> implements Function<Message, T> {
    private Type<?> type;
    private ObjectMapper objectMapper;

    /**
     * Constructor.
     * @param type The type to deserialize to.
     * @param objectMapper serializer to convert Elide models to topic messages.
     */
    public MessageDeserializer(Type<?> type, ObjectMapper objectMapper) {
        this.type = type;
        this.objectMapper = objectMapper;
    }

    @Override
    public T apply(Message message) {
        try {
            return (T) objectMapper.readValue(((TextMessage) message).getText(), type.getUnderlyingClass().get());
        } catch (JsonProcessingException | JMSException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
