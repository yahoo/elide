/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.resources.JsonApiEndpoint;
import com.yahoo.elide.security.executors.VerbosePermissionExecutor;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.File;
import java.util.HashMap;
import java.util.TimeZone;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

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

                EntityManagerFactory emf = Persistence.createEntityManagerFactory("searchDataStoreTest");
                DataStore jpaStore = new JpaDataStore(
                        emf::createEntityManager,
                        NonJtaTransaction::new);

                EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

                DataStore searchStore = new SearchDataStore(jpaStore, emf, indexOnStartup, 3, 50);
                jpaStore.populateEntityDictionary(dictionary);
                searchStore.populateEntityDictionary(dictionary);

                Elide elide = new Elide(new ElideSettingsBuilder(searchStore)
                        .withAuditLogger(new Slf4jLogger())
                        .withEntityDictionary(dictionary)
                        .withPermissionExecutor(VerbosePermissionExecutor::new)
                        .withDefaultMaxPageSize(Pagination.MAX_PAGE_LIMIT)
                        .withDefaultPageSize(Pagination.DEFAULT_PAGE_LIMIT)
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))
                        .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                        .withJoinFilterDialect(new DefaultFilterDialect(dictionary))
                        .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                        .withSubqueryFilterDialect(new DefaultFilterDialect(dictionary))
                        .build());

                bind(elide).to(Elide.class).named("elide");
                bind(JsonApiEndpoint.DEFAULT_GET_USER)
                        .to(DefaultOpaqueUserFunction.class)
                        .named("elideUserExtractionFunction");
            }
        });
    }
}
