/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jms;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.graphql.subscriptions.hooks.SubscriptionScanner;
import com.paiondata.elide.jsonapi.JsonApiSettings;

import example.Author;
import example.Book;
import example.ChatBot;
import example.Publisher;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import jakarta.jms.ConnectionFactory;

import java.util.Calendar;
import java.util.Set;

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
                        .entityDictionary(elide.getElideSettings().getEntityDictionary())
                        .scanner(elide.getScanner())
                        .objectMapper(elide.getElideSettings().getObjectMapper())
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

        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder().joinFilterDialect(rsqlFilterStrategy)
                .subqueryFilterDialect(rsqlFilterStrategy);
        return new Elide(ElideSettings.builder().dataStore(store)
                .auditLogger(auditLogger)
                .settings(jsonApiSettings)
                .entityDictionary(dictionary)
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone()))
                .build());
    }
}
