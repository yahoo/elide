/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paiondata.elide.core.type.ClassType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.TextMessage;

import java.util.Iterator;

public class MessageIterableTest {

    @Test
    public void testIteratorMultipleItems() throws Exception {
        JMSConsumer consumer = mock(JMSConsumer.class);
        TextMessage msg1 = mock(TextMessage.class);
        when(msg1.getText()).thenReturn("1");
        TextMessage msg2 = mock(TextMessage.class);
        when(msg2.getText()).thenReturn("2");
        TextMessage msg3 = mock(TextMessage.class);
        when(msg3.getText()).thenReturn("3");

        when(consumer.receive(anyLong()))
                .thenReturn(msg1)
                .thenReturn(msg2)
                .thenReturn(msg3)
                .thenThrow(new JMSRuntimeException("timeout"));

        Iterator<String> iterator = new MessageIterable(
                consumer,
                1000,
                new MessageDeserializer(ClassType.of(String.class), new ObjectMapper())).iterator();

        assertTrue(iterator.hasNext());
        assertEquals("1", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("2", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("3", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testZeroTimeout() throws Exception {
        JMSConsumer consumer = mock(JMSConsumer.class);
        TextMessage msg1 = mock(TextMessage.class);
        when(msg1.getText()).thenReturn("1");

        when(consumer.receiveNoWait())
                .thenReturn(msg1)
                .thenReturn(null);

        Iterator<String> iterator = new MessageIterable(
                consumer,
                0,
                new MessageDeserializer(ClassType.of(String.class), new ObjectMapper())).iterator();

        assertTrue(iterator.hasNext());
        assertEquals("1", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testNegativeTimeout() throws Exception {
        JMSConsumer consumer = mock(JMSConsumer.class);
        TextMessage msg1 = mock(TextMessage.class);
        when(msg1.getText()).thenReturn("1");

        when(consumer.receive())
                .thenReturn(msg1)
                .thenReturn(null);

        Iterator<String> iterator = new MessageIterable(
                consumer,
                -1,
                new MessageDeserializer(ClassType.of(String.class), new ObjectMapper())).iterator();

        assertTrue(iterator.hasNext());
        assertEquals("1", iterator.next());
        assertFalse(iterator.hasNext());
    }
}
