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
            Message next;

            @Override
            public boolean hasNext() {
                next = (Message) next();
                if (next == null) {
                    return false;
                }

                return true;
            }

            @Override
            public T next() {
                try {
                    Message message = consumer.receive(timeout);
                    if (message != null) {
                        return messageConverter.apply(message);
                    }
                    return null;
                }
                catch (JMSRuntimeException e)  {
                    return null;
                }
            }
        };
    }
}
