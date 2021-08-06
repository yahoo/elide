/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import java.util.Iterator;
import java.util.function.Function;
import javax.jms.JMSConsumer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;

/**
 * Converts a JMS message consumer into an Iterable.
 * @param <T> The type to convert a Message into.
 */
public class MessageIterator<T> implements Iterable<T> {

    private JMSConsumer consumer;
    private long timeout;
    private Function<Message, T> messageConverter;

    /**
     * Constructor.
     * @param consumer The JMS message consumer to convert to an interator.
     * @param timeout The timeout to wait on message topics.  0 means no wait.  Less than 0 means wait forever.
     * @param messageConverter Converts JMS messages into some other thing.
     */
    public MessageIterator(
            JMSConsumer consumer,
            long timeout,
            Function<Message, T> messageConverter
    ) {
        this.consumer = consumer;
        this.timeout = timeout;
        this.messageConverter = messageConverter;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator() {
            T next;

            @Override
            public boolean hasNext() {
                next = next();
                if (next == null) {
                    return false;
                }

                return true;
            }

            @Override
            public T next() {
                if (next != null) {
                    T result = next;
                    next = null;
                    return result;
                }

                try {
                    Message message;
                    if (timeout == 0) {
                        message = consumer.receiveNoWait();
                    } else if (timeout > 0) {
                        message = consumer.receive(timeout);
                    } else {
                        message = consumer.receive();
                    }

                    if (message != null) {
                        return messageConverter.apply(message);
                    }
                    return null;
                } catch (JMSRuntimeException e)  {
                    return null;
                }
            }
        };
    }
}
