/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores;

import com.yahoo.elide.core.DataStoreTransaction;

import example.Parent;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;

/**
 * The type Config resource test.
 */
public class ExampleIT extends AHibernateTest {


    @Test
    public void verifySession() throws IOException {
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            tx.commit();
        }
    }

    @Test
    public void accessParentBean() {
        DataStoreTransaction tx = dataStore.beginTransaction();
        Parent parent = new Parent();
        parent.setChildren(new HashSet<>());
        parent.setSpouses(new HashSet<>());
        tx.save(parent);
        tx.commit();
    }
}
