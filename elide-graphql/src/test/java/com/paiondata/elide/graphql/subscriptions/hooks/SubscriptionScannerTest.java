/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.hooks;

import static com.paiondata.elide.core.PersistentResource.CLASS_NO_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Author;
import example.Book;
import org.junit.jupiter.api.Test;

import jakarta.jms.ConnectionFactory;

public class SubscriptionScannerTest {

    @Test
    public void testLifeCycleHookBindings() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ClassScanner classScanner = new DefaultClassScanner();
        EntityDictionary dictionary = EntityDictionary.builder().scanner(classScanner).build();

        SubscriptionScanner subscriptionScanner = SubscriptionScanner.builder()
                .connectionFactory(connectionFactory)
                .entityDictionary(dictionary)
                .scanner(classScanner)
                .objectMapper(new ObjectMapper())
                .build();

        subscriptionScanner.bindLifecycleHooks();

        assertEquals(1, dictionary.getTriggers(ClassType.of(Book.class),
                LifeCycleHookBinding.Operation.CREATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                CLASS_NO_FIELD).size());

        assertEquals(0, dictionary.getTriggers(ClassType.of(Book.class),
                LifeCycleHookBinding.Operation.DELETE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                CLASS_NO_FIELD).size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Book.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "title").size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Book.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "genre").size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Book.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "authors").size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Book.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "previews").size());

        assertEquals(0, dictionary.getTriggers(ClassType.of(Book.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "price").size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Author.class),
                LifeCycleHookBinding.Operation.CREATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                CLASS_NO_FIELD).size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Author.class),
                LifeCycleHookBinding.Operation.DELETE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                CLASS_NO_FIELD).size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Author.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "name").size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Author.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "type").size());

        assertEquals(1, dictionary.getTriggers(ClassType.of(Author.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "homeAddress").size());

        assertEquals(0, dictionary.getTriggers(ClassType.of(Author.class),
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.POSTCOMMIT,
                "birthDate").size());
    }
}
