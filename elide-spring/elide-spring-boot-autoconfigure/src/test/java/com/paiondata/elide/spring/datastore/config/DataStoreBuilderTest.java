/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.datastore.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;

import example.models.jpa.ArtifactGroup;
import example.models.jpa.ArtifactProduct;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for DataStoreBuilder.
 */
class DataStoreBuilderTest {

    @Test
    void dataStore() {
        DataStoreBuilder builder = new DataStoreBuilder();
        builder.dataStore(new HashMapDataStore(Arrays.asList(ArtifactGroup.class)));
        assertThat(builder.build()).isInstanceOf(HashMapDataStore.class);
    }

    @Test
    void dataStores() {
        DataStoreBuilder builder = new DataStoreBuilder();
        builder.dataStores(Collections.singletonList(new HashMapDataStore(Arrays.asList(ArtifactGroup.class))));
        assertThat(builder.build()).isInstanceOf(HashMapDataStore.class);
    }

    @Test
    void dataStoresCustomizer() {
        DataStoreBuilder builder = new DataStoreBuilder();
        builder.dataStore(new HashMapDataStore(Arrays.asList(ArtifactGroup.class)));
        builder.dataStores(dataStores -> dataStores.clear());
        assertThat(builder.build()).isNull();
    }

    @Test
    void multiplexer() {
        DataStoreBuilder builder = new DataStoreBuilder();
        builder.dataStore(new HashMapDataStore(Arrays.asList(ArtifactGroup.class)));
        builder.dataStore(new HashMapDataStore(Arrays.asList(ArtifactProduct.class)));
        builder.multiplexer(dataStores -> new CustomMultiplexManager());
        assertThat(builder.build()).isInstanceOf(CustomMultiplexManager.class);
    }

    public static class CustomMultiplexManager implements DataStore {

        @Override
        public void populateEntityDictionary(EntityDictionary dictionary) {
        }

        @Override
        public DataStoreTransaction beginTransaction() {
            return null;
        }
    }
}
