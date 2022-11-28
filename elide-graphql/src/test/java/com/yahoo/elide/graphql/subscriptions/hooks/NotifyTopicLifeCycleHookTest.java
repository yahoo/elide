/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.lifecycle.CRUDEvent;
import com.google.gson.GsonBuilder;
import example.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Topic;

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
                JMSContext::createProducer, new GsonBuilder().create());

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
