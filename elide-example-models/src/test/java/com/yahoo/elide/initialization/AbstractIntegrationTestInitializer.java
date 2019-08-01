/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import org.glassfish.jersey.server.ResourceConfig;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Supplier;

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
    protected final JsonApiMapper jsonApiMapper = new JsonApiMapper(new EntityDictionary(new HashMap<>()));

    public AbstractIntegrationTestInitializer() {
        super();
    }

    protected AbstractIntegrationTestInitializer(final Class<? extends ResourceConfig> resourceConfig) {
        super(resourceConfig);
    }

    /**
     * Gets database manager.
     *
     * @return the database manager
     */
    public static DataStore getDatabaseManager() {
        if (dataStore == null) {
            return getNewDatabaseManager();
        }
        return dataStore;
    }

    public static DataStore getNewDatabaseManager() {
        try {
            final String dataStoreSupplierName = System.getProperty("dataStoreSupplier");
            @SuppressWarnings("unchecked")
            Supplier<DataStore> dataStoreSupplier =
                    Class.forName(dataStoreSupplierName).asSubclass(Supplier.class).newInstance();
            dataStore = dataStoreSupplier.get();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalStateException(e);
        }
        return dataStore;
    }

        /**
         * Hibernate init.
         */
    @BeforeClass
    public static void hibernateInit() {
        getNewDatabaseManager();
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
