/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.tests;

import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.initialization.IntegrationTest;
import example.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;

/**
 * Simple integration tests to verify session and access.
 */
public class AccessIT extends IntegrationTest {

    @BeforeAll
    public void setup() {
        dataStore.populateEntityDictionary(EntityDictionary.builder().build());
    }

    @Test
    public void verifySession() throws IOException {
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            tx.commit(null);
        }
    }

    @Test
    public void accessParentBean() throws IOException {
        DataStoreTransaction tx = dataStore.beginTransaction();
        Parent parent = new Parent();
        parent.setChildren(new HashSet<>());
        parent.setSpouses(new HashSet<>());
        tx.createObject(parent, null);
        tx.commit(null);
        tx.close();
    }
}
