/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.jsonapi.JsonApiSettings;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.File;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * Jetty resource configuration that configures dependency injection.
 */
public class DependencyBinder extends ResourceConfig {
    public DependencyBinder() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {

                //We can only create the index once.
                boolean indexOnStartup = false;
                File file = new File("/tmp/lucene");

                if (! file.exists()) {
                    file.mkdirs();
                    indexOnStartup = true;
                }

                Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();
                EntityManagerFactory emf = Persistence.createEntityManagerFactory("searchDataStoreTest");
                DataStore jpaStore = new JpaDataStore(
                        emf::createEntityManager,
                        em -> new NonJtaTransaction(em, txCancel), emf::getMetamodel);

                EntityDictionary dictionary = EntityDictionary.builder().build();

                DataStore searchStore = new SearchDataStore(jpaStore, emf, indexOnStartup, 3, 50);
                jpaStore.populateEntityDictionary(dictionary);
                searchStore.populateEntityDictionary(dictionary);

                JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder()
                        .joinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                        .joinFilterDialect(new DefaultFilterDialect(dictionary))
                        .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                        .subqueryFilterDialect(new DefaultFilterDialect(dictionary));

                Elide elide = new Elide(ElideSettings.builder().dataStore(searchStore)
                        .auditLogger(new Slf4jLogger())
                        .entityDictionary(dictionary)
                        .verboseErrors(true)
                        .defaultMaxPageSize(PaginationImpl.MAX_PAGE_LIMIT)
                        .defaultPageSize(PaginationImpl.DEFAULT_PAGE_LIMIT)
                        .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")))
                        .settings(jsonApiSettings)
                        .build());

                bind(elide).to(Elide.class).named("elide");
            }
        });
    }
}
