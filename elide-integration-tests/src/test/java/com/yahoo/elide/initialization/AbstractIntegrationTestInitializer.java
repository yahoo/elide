/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import org.testng.annotations.BeforeTest;

import java.io.IOException;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Integration test initializer.
 *
 */
public abstract class AbstractIntegrationTestInitializer extends AbstractApiResourceInitializer {
    /**
     * The constant dataStore.
     */
    public static DataStore dataStore = null;
    /**
     * The Json api mapper.
     * Empty dictionary is OK provided the OBJECT_MAPPER is used for reading only
     */
    protected final JsonApiMapper jsonApiMapper = new JsonApiMapper(new EntityDictionary());

    /**
     * Gets database manager.
     *
     * @return the database manager
     */
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

    protected AbstractIntegrationTestInitializer() {
    }

    /**
     * Hibernate init.
     */
    @BeforeTest
    public static void hibernateInit() {
        getDatabaseManager();
    }

    /**
     * Assert equal documents.
     *
     * @param actual   the actual
     * @param expected the expected
     */
    protected void assertEqualDocuments(final String actual, final String expected) {
        try {
            JsonApiDocument expectedDoc = jsonApiMapper.readJsonApiDocument(expected);
            JsonApiDocument actualDoc = jsonApiMapper.readJsonApiDocument(actual);
            assertEquals(actualDoc, expectedDoc, "\n" + actual + "\n" + expected + "\n");
        } catch (IOException e) {
            fail("\n" + actual + "\n" + expected + "\n", e);
        }
    }
}
