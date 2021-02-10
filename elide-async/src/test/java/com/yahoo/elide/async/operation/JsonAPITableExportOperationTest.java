/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.export.formatter.JSONExportFormatter;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
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
        dataStore = new HashMapDataStore(TableExport.class.getPackage());
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
                        .withEntityDictionary(new EntityDictionary(map))
                        .withAuditLogger(new Slf4jLogger())
                        .withDownloadApiPath("/export")
                        .build());
        user = mock(User.class);
        requestScope = mock(RequestScope.class);
        asyncExecutorService = mock(AsyncExecutorService.class);
        engine = new FileResultStorageEngine(tempDir.toString());
        when(asyncExecutorService.getElide()).thenReturn(elide);
        when(asyncExecutorService.getResultStorageEngine()).thenReturn(engine);
        when(requestScope.getApiVersion()).thenReturn(NO_VERSION);
        when(requestScope.getUser()).thenReturn(user);
        when(requestScope.getElideSettings()).thenReturn(elide.getElideSettings());
        when(requestScope.getBaseUrlEndPoint()).thenReturn("https://elide.io");
    }

    @Test
    public void testProcessQuery() throws URISyntaxException, IOException  {
        dataPrep();
        TableExport queryObj = new TableExport();
        String query = "/tableExport?sort=principalName&fields=principalName";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.CSV);

        JSONAPITableExportOperation jsonAPIOperation = new JSONAPITableExportOperation(new JSONExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope);
        TableExportResult queryResultObj = (TableExportResult) jsonAPIOperation.call();

        assertEquals(200, queryResultObj.getHttpStatus());
        assertTrue("https://elide.io/export/edc4a871-dff2-4054-804e-d80075cf827d".equals(queryResultObj.getUrl().toString()));
        assertEquals(1, queryResultObj.getRecordCount());
    }

    @Test
    public void testProcessBadEntityQuery() throws URISyntaxException, IOException  {
        dataPrep();
        TableExport queryObj = new TableExport();
        String query = "/tableExportInvalid?sort=principalName&fields=principalName";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.CSV);

        JSONAPITableExportOperation jsonAPIOperation = new JSONAPITableExportOperation(new JSONExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope);
        TableExportResult queryResultObj = (TableExportResult) jsonAPIOperation.call();

        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals("Unknown collection tableExportInvalid", queryResultObj.getMessage());
    }

    @Test
    public void testProcessBadQuery() throws URISyntaxException, IOException  {
        dataPrep();
        TableExport queryObj = new TableExport();
        String query = "tableExport/^IllegalCharacter^";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.CSV);

        JSONAPITableExportOperation jsonAPIOperation = new JSONAPITableExportOperation(new JSONExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope);
        TableExportResult queryResultObj = (TableExportResult) jsonAPIOperation.call();

        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals("EntityProjection generation failure.", queryResultObj.getMessage());
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
