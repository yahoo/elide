/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.jpql.porting.QueryLogger;
import com.yahoo.elide.initialization.IntegrationTest;
import org.mockito.ArgumentCaptor;

import java.util.List;

/**
 * Base integration test for tests that want to verify JPQL issued by the JPA Data Store.
 */
public abstract class JPQLIntegrationTest extends IntegrationTest {
    protected QueryLogger logger;

    public JPQLIntegrationTest() {
        super();
        initializeLogger();
    }

    private void initializeLogger() {
        if (logger == null) {
            logger = mock(QueryLogger.class);
        }
    }

    @Override
    protected DataStoreTestHarness createHarness() {
        initializeLogger();
        return new JpaDataStoreHarness(logger, delegateToInMemoryStore());
    }

    protected boolean delegateToInMemoryStore() {
        return false;
    }

    protected void verifyLoggingStatements(String ... statements) {
        ArgumentCaptor<String> actual = ArgumentCaptor.forClass(String.class);
        verify(logger, times(statements.length)).log(actual.capture());
        List<String> actualAllValues = actual.getAllValues();
        int idx = 0;
        for (String statement : statements) {
            assertEquals(normalizeQuery(statement), normalizeQuery(actualAllValues.get(idx)),
                    String.format("Query %s Mismatch", idx));
            idx++;
        }
    }

    private static String normalizeQuery(String query) {
        String normalized = query.replaceAll("Query Hash: \\d+\tHQL Query: ", "");
        normalized = normalized.replaceAll(":\\w+", ":XXX");
        normalized = normalized.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }
}
