/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import org.testng.annotations.BeforeTest;

import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Supplier;

@NoArgsConstructor
public abstract class AbstractHibernateTestService {
    public static DataStore dataStore = null;

    /* Empty dictionary is OK provided the OBJECT_MAPPER is used for reading only */
    protected final JsonApiMapper jsonApiMapper = new JsonApiMapper(new EntityDictionary(new HashMap<>()));

    public static DataStore getDatabaseManager() {
        if (dataStore == null) {
            try {
                final String dataStoreSupplierName = System.getProperty("dataStoreSupplier");
                @SuppressWarnings("unchecked")
                Supplier<DataStore> dataStoreSupplier =
                        Class.forName(dataStoreSupplierName).asSubclass(Supplier.class).newInstance();
                dataStore = dataStoreSupplier.get();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
                throw new IllegalStateException(e);
            }
        }

        return dataStore;
    }

    @BeforeTest
    public static void initHibernate() {
        getDatabaseManager();
    }

    protected void assertEqualDocuments(String actual, String expected) {
        try {
            JsonApiDocument expectedDoc = jsonApiMapper.readJsonApiDocument(expected);
            JsonApiDocument actualDoc = jsonApiMapper.readJsonApiDocument(actual);
            assertEquals(actualDoc, expectedDoc, "\n" + actual + "\n" + expected + "\n");
        } catch (IOException e) {
            fail("\n" + actual + "\n" + expected + "\n", e);
        }
    }
}
