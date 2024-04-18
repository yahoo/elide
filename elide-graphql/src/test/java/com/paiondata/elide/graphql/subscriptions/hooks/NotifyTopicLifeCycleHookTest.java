/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.lifecycle.CRUDEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Topic;

import java.util.Optional;

public class NotifyTopicLifeCycleHookTest {

    private ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    private JMSContext context = mock(JMSContext.class);
    private JMSProducer producer = mock(JMSProducer.class);
    private RequestScope scope = mock(RequestScope.class);

    @BeforeEach
    public void setup() {
        EntityDictionary dictionary;
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);

        Topic destination = mock(Topic.class);
        reset(scope);
        reset(connectionFactory);
        reset(producer);
        when(connectionFactory.createContext()).thenReturn(context);
        when(context.createProducer()).thenReturn(producer);
        when(context.createTopic(any())).thenReturn(destination);
        when(scope.getDictionary()).thenReturn(dictionary);
    }

    @Test
    public void testManagedModelNotification() {

        NotifyTopicLifeCycleHook<Book> bookHook = new NotifyTopicLifeCycleHook<Book>(
                connectionFactory,
                new ObjectMapper(), JMSContext::createProducer);

        Book book = new Book();
        PersistentResource<Book> resource = new PersistentResource<>(book, "123", scope);

        bookHook.execute(LifeCycleHookBinding.Operation.CREATE, LifeCycleHookBinding.TransactionPhase.PRECOMMIT,
                new CRUDEvent(
                        LifeCycleHookBinding.Operation.CREATE,
                        resource,
                        "",
                        Optional.empty()
                ));

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).createTopic(topicCaptor.capture());
        assertEquals("bookAdded", topicCaptor.getValue());
        verify(producer, times(1)).send(isA(Destination.class), isA(String.class));
    }
}
