/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paiondata.elide.core.type.ClassType;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Book;
import org.junit.jupiter.api.Test;

import jakarta.jms.TextMessage;

public class MessageDeserializerTest {

    @Test
    public void testDeserialization() throws Exception {
        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{ \"title\": \"Foo\", \"id\" : 123 }");

        MessageDeserializer<Book> deserializer = new MessageDeserializer(ClassType.of(Book.class),
                new ObjectMapper());

        Book book = deserializer.apply(message);
        assertEquals("Foo", book.getTitle());
        assertEquals(123, book.getId());
    }
}
