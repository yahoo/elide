/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionScanner;
import example.Author;
import example.Book;
import example.ChatBot;
import example.Publisher;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.util.Calendar;
import java.util.Set;

import javax.jms.ConnectionFactory;

/**
 * HK2 Binder for the Integration test.
 */
public class TestBinder extends AbstractBinder {

    public static final String EMBEDDED_JMS_URL = "vm://0";

    private final AuditLogger auditLogger;
    private final ServiceLocator injector;

    public TestBinder(final AuditLogger auditLogger, final ServiceLocator injector) {
        this.auditLogger = auditLogger;
        this.injector = injector;
    }

    @Override
    protected void configure() {
        EntityDictionary dictionary = EntityDictionary.builder()
                .injector(injector::inject)
                .build();

        dictionary.scanForSecurityChecks();

        bind(dictionary).to(EntityDictionary.class);

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(EMBEDDED_JMS_URL);

        bind(connectionFactory).to(ConnectionFactory.class);

        // Primary Elide instance for CRUD endpoints.
        bindFactory(new Factory<Elide>() {
            @Override
            public Elide provide() {

                HashMapDataStore inMemoryStore = new HashMapDataStore(
                        Set.of(Book.class, Author.class, Publisher.class, ChatBot.class)
                );
                Elide elide = buildElide(inMemoryStore, dictionary);

                elide.doScans();

                SubscriptionScanner subscriptionScanner = SubscriptionScanner.builder()
                        .connectionFactory(connectionFactory)
                        .dictionary(elide.getElideSettings().getDictionary())
                        .scanner(elide.getScanner())
                        .build();

                subscriptionScanner.bindLifecycleHooks();
                return elide;
            }

            @Override
            public void dispose(Elide elide) {

            }
        }).to(Elide.class).named("elide");
    }

    protected Elide buildElide(DataStore store, EntityDictionary dictionary) {
        RSQLFilterDialect rsqlFilterStrategy = RSQLFilterDialect.builder().dictionary(dictionary).build();

        return new Elide(new ElideSettingsBuilder(store)
                .withAuditLogger(auditLogger)
                .withJoinFilterDialect(rsqlFilterStrategy)
                .withSubqueryFilterDialect(rsqlFilterStrategy)
                .withEntityDictionary(dictionary)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
                .build());
    }
}
