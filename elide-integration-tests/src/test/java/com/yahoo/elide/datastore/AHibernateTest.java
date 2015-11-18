/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastore;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.endpoints.AbstractApiResourceTest;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import org.hibernate.SessionFactory;
import org.testng.annotations.BeforeTest;

import java.io.IOException;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * This class provides some helper functions for tests to start and end transactions. Any test that fails to terminate
 * its own transaction will have it rolled back at the end.
 */
public abstract class AHibernateTest extends AbstractApiResourceTest {
    protected static volatile SessionFactory sessionFactory;
    public static DataStore dataStore = null;

    /* Empty dictionary is OK provided the OBJECT_MAPPER is used for reading only */
    protected final JsonApiMapper mapper = new JsonApiMapper(new EntityDictionary());

    public static DataStore getDatabaseManager() {
        if (dataStore == null) {
            try {
                final String dataStoreSupplierName = System.getProperty("dataStoreSupplier");
                Supplier<DataStore> dataStoreSupplier =
                        (Supplier<DataStore>) Class.forName(dataStoreSupplierName).newInstance();
                dataStore = dataStoreSupplier.get();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        return dataStore;
    }

    protected AHibernateTest() {
    }

    @BeforeTest
    public static void hibernateInit() {
       getDatabaseManager();
    }

    protected void assertEqualDocuments(String actual, String expected) {
        try {
            JsonApiDocument expectedDoc = mapper.readJsonApiDocument(expected);
            JsonApiDocument actualDoc = mapper.readJsonApiDocument(actual);
            assertEquals(actualDoc, expectedDoc, "\n" + actual + "\n" + expected + "\n");
        } catch (IOException e) {
            fail("\n" + actual + "\n" + expected + "\n", e);
        }
    }
}
