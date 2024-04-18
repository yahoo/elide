/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;

import example.Book;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Verifies Hibernate caching.
 */
public class CacheIT {
    private JpaDataStoreHarness dataStoreHarness;
    private ElideSettings elideSettings;

    public CacheIT() {
        /*
         * JAKARTA_SHARED_CACHE_MODE is set to ALL. If set to ENABLE_SELECTIVE the
         * entity needs the Cacheable annotation.
         */
        this.dataStoreHarness = new JpaDataStoreHarness(Map.of(AvailableSettings.USE_SECOND_LEVEL_CACHE, "true",
                AvailableSettings.CACHE_REGION_FACTORY, "jcache", AvailableSettings.GENERATE_STATISTICS, "true",
                AvailableSettings.JAKARTA_SHARED_CACHE_MODE, "ALL"));
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        entityDictionary.bindEntity(Book.class);
        elideSettings = ElideSettings.builder().entityDictionary(entityDictionary).build();
    }

    @AfterEach
    public void afterEach() {
        dataStoreHarness.cleanseTestData();
    }

    @Test
    void shouldHaveSecondLevelCacheHit() throws IOException {
        DataStore dataStore = dataStoreHarness.getDataStore();
        Statistics statistics = dataStoreHarness.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        long book1Id;
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            Book book1 = new Book();
            book1.setTitle("Test Book1");
            tx.createObject(book1, null);
            tx.commit(null);
            book1Id = book1.getId();
        }

        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            RequestScope requestScope = RequestScope.builder().elideSettings(elideSettings)
                    .route(Route.builder().build()).build();
            Book loaded = tx.loadObject(EntityProjection.builder().type(Book.class).build(), Long.valueOf(book1Id),
                    requestScope);
            assertEquals(book1Id, loaded.getId());
            assertEquals(0, statistics.getSecondLevelCacheHitCount());
            assertEquals(1, statistics.getSecondLevelCachePutCount());
            assertEquals(1, statistics.getSecondLevelCacheMissCount());
            statistics.clear();
        }

        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            RequestScope requestScope = RequestScope.builder().elideSettings(elideSettings)
                    .route(Route.builder().build()).build();
            Book loaded = tx.loadObject(EntityProjection.builder().type(Book.class).build(), Long.valueOf(book1Id),
                    requestScope);
            assertEquals(book1Id, loaded.getId());
            assertEquals(1, statistics.getSecondLevelCacheHitCount());
            assertEquals(0, statistics.getSecondLevelCachePutCount());
            assertEquals(0, statistics.getSecondLevelCacheMissCount());
            tx.delete(loaded, requestScope);
            statistics.clear();
        }
    }
}
