/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.type.Type;
import com.google.gson.Gson;

import java.util.function.Function;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

/**
 * Converts JMS messages to elide model instances via Jackson.
 * @param <T> elide model type.
 */
public class MessageDeserializer<T> implements Function<Message, T> {
    private Type<?> type;
    private Gson gson;

    /**
     * Constructor.
     * @param type The type to deserialize to.
     * @param gson Gson serializer to convert Elide models to topic messages.
     */
    public MessageDeserializer(Type<?> type, Gson gson) {
        this.type = type;
        this.gson = gson;
    }

    @Override
    public T apply(Message message) {
        try {
            return (T) gson.fromJson(((TextMessage) message).getText(), type.getUnderlyingClass().get());
        } catch (JMSException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
