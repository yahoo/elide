/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.export.formatter.JSONExportFormatter;
import com.yahoo.elide.async.models.ArtifactGroup;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.models.security.AsyncAPIInlineChecks;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class JsonAPITableExportOperationTest {

    private HashMapDataStore dataStore;
    private User user;
    private RequestScope requestScope;
    private Elide elide;
    private AsyncExecutorService asyncExecutorService;
    private ResultStorageEngine engine;

    @BeforeEach
    public void setupMocks(@TempDir Path tempDir) {
        dataStore = new HashMapDataStore(DefaultClassScanner.getInstance(),
                        new HashSet<>(Arrays.asList(TableExport.class.getPackage(), ArtifactGroup.class.getPackage())));
        Map<String, Class<? extends Check>> map = new HashMap<>();
        map.put(AsyncAPIInlineChecks.AsyncAPIOwner.PRINCIPAL_IS_OWNER,
                AsyncAPIInlineChecks.AsyncAPIOwner.class);
        map.put(AsyncAPIInlineChecks.AsyncAPIAdmin.PRINCIPAL_IS_ADMIN,
                AsyncAPIInlineChecks.AsyncAPIAdmin.class);
        map.put(AsyncAPIInlineChecks.AsyncAPIStatusValue.VALUE_IS_CANCELLED,
                AsyncAPIInlineChecks.AsyncAPIStatusValue.class);
        map.put(AsyncAPIInlineChecks.AsyncAPIStatusQueuedValue.VALUE_IS_QUEUED,
                AsyncAPIInlineChecks.AsyncAPIStatusQueuedValue.class);

        elide = new Elide(
                    new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(EntityDictionary.builder().checks(map).build())
                        .withAuditLogger(new Slf4jLogger())
                        .withExportApiPath("/export")
                        .build());
        elide.doScans();
        user = mock(User.class);
        requestScope = mock(RequestScope.class);
        asyncExecutorService = mock(AsyncExecutorService.class);
        engine = new FileResultStorageEngine(tempDir.toString(), true);
        when(asyncExecutorService.getElide()).thenReturn(elide);
        when(requestScope.getApiVersion()).thenReturn(NO_VERSION);
        when(requestScope.getUser()).thenReturn(user);
        when(requestScope.getElideSettings()).thenReturn(elide.getElideSettings());
        when(requestScope.getBaseUrlEndPoint()).thenReturn("https://elide.io");
    }

    @Test
    public void testProcessQuery() throws IOException  {
        dataPrep();
        TableExport queryObj = new TableExport();
        String query = "/tableExport?sort=principalName&fields=principalName";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.CSV);

        JSONAPITableExportOperation jsonAPIOperation = new JSONAPITableExportOperation(new JSONExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope, engine);
        TableExportResult queryResultObj = (TableExportResult) jsonAPIOperation.call();

        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals("https://elide.io/export/edc4a871-dff2-4054-804e-d80075cf827d.csv", queryResultObj.getUrl().toString());
        assertEquals(1, queryResultObj.getRecordCount());
        assertNull(queryResultObj.getMessage());
    }

    @Test
    public void testProcessBadEntityQuery() throws IOException  {
        dataPrep();
        TableExport queryObj = new TableExport();
        String query = "/tableExportInvalid?sort=principalName&fields=principalName";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.CSV);

        JSONAPITableExportOperation jsonAPIOperation = new JSONAPITableExportOperation(new JSONExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope, engine);
        TableExportResult queryResultObj = (TableExportResult) jsonAPIOperation.call();

        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals("Unknown collection tableExportInvalid", queryResultObj.getMessage());
    }

    @Test
    public void testProcessBadQuery() throws IOException  {
        dataPrep();
        TableExport queryObj = new TableExport();
        String query = "tableExport/^IllegalCharacter^";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.CSV);

        JSONAPITableExportOperation jsonAPIOperation = new JSONAPITableExportOperation(new JSONExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope, engine);
        TableExportResult queryResultObj = (TableExportResult) jsonAPIOperation.call();

        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals("Illegal character in path at index 12: tableExport/^IllegalCharacter^",
                        queryResultObj.getMessage());
    }

    @Test
    public void testProcessQueryWithRelationship() {
        TableExport queryObj = new TableExport();
        String query = "/group?fields[group]=products";
        String id = "edc4a871-dff2-4194-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.CSV);

        JSONAPITableExportOperation jsonAPIOperation = new JSONAPITableExportOperation(new JSONExportFormatter(elide),
                        asyncExecutorService, queryObj, requestScope, engine);
        TableExportResult queryResultObj = (TableExportResult) jsonAPIOperation.call();

        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals("Export is not supported for Query that requires traversing Relationships.",
                        queryResultObj.getMessage());
        assertNull(queryResultObj.getRecordCount());
        assertNull(queryResultObj.getUrl());
    }

    /**
     * Prepping and Storing an TableExport entry to be queried later on.
     * @throws IOException  IOException
     */
    private void dataPrep() throws IOException {
        TableExport temp = new TableExport();
        DataStoreTransaction tx = dataStore.beginTransaction();
        RequestScope scope = new RequestScope(null, null, NO_VERSION, null, tx, user, null, Collections.emptyMap(),
                UUID.randomUUID(), elide.getElideSettings());
        tx.save(temp, scope);
        tx.commit(scope);
        tx.close();
    }

    @AfterEach
    public void clearDataStore() {
        dataStore.cleanseTestData();
    }
}
