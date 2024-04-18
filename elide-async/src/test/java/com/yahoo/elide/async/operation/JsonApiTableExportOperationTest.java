/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.operation;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.AsyncSettings;
import com.paiondata.elide.async.DefaultResultTypeFileExtensionMapper;
import com.paiondata.elide.async.export.formatter.JsonExportFormatter;
import com.paiondata.elide.async.models.ArtifactGroup;
import com.paiondata.elide.async.models.QueryType;
import com.paiondata.elide.async.models.ResultType;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.models.TableExportResult;
import com.paiondata.elide.async.models.security.AsyncApiInlineChecks;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.async.service.storageengine.FileResultStorageEngine;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.audit.Slf4jLogger;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.jsonapi.JsonApiSettings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class JsonApiTableExportOperationTest {

    private HashMapDataStore dataStore;
    private User user;
    private RequestScope requestScope;
    private Elide elide;
    private AsyncExecutorService asyncExecutorService;
    private ResultStorageEngine engine;
    private DefaultResultTypeFileExtensionMapper resultTypeFileExtensionMapper = new DefaultResultTypeFileExtensionMapper();

    @BeforeEach
    public void setupMocks(@TempDir Path tempDir) {
        dataStore = new HashMapDataStore(new DefaultClassScanner(),
                        new HashSet<>(Arrays.asList(TableExport.class.getPackage(), ArtifactGroup.class.getPackage())));
        Map<String, Class<? extends Check>> map = new HashMap<>();
        map.put(AsyncApiInlineChecks.AsyncApiOwner.PRINCIPAL_IS_OWNER,
                AsyncApiInlineChecks.AsyncApiOwner.class);
        map.put(AsyncApiInlineChecks.AsyncApiAdmin.PRINCIPAL_IS_ADMIN,
                AsyncApiInlineChecks.AsyncApiAdmin.class);
        map.put(AsyncApiInlineChecks.AsyncApiStatusValue.VALUE_IS_CANCELLED,
                AsyncApiInlineChecks.AsyncApiStatusValue.class);
        map.put(AsyncApiInlineChecks.AsyncApiStatusQueuedValue.VALUE_IS_QUEUED,
                AsyncApiInlineChecks.AsyncApiStatusQueuedValue.class);

        elide = new Elide(
                    ElideSettings.builder().dataStore(dataStore)
                        .entityDictionary(EntityDictionary.builder().checks(map).build())
                        .auditLogger(new Slf4jLogger())
                        .settings(AsyncSettings.builder().export(export -> export.path("/export")))
                        .settings(JsonApiSettings.builder())
                        .build());
        elide.doScans();
        user = mock(User.class);
        requestScope = mock(RequestScope.class);
        asyncExecutorService = mock(AsyncExecutorService.class);
        engine = new FileResultStorageEngine(tempDir.toString());
        when(asyncExecutorService.getElide()).thenReturn(elide);
        when(requestScope.getRoute()).thenReturn(Route.builder().apiVersion(NO_VERSION).baseUrl("https://elide.io").build());
        when(requestScope.getUser()).thenReturn(user);
        when(requestScope.getElideSettings()).thenReturn(elide.getElideSettings());
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

        JsonApiTableExportOperation jsonAPIOperation = new JsonApiTableExportOperation(new JsonExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope, engine, resultTypeFileExtensionMapper);
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

        JsonApiTableExportOperation jsonAPIOperation = new JsonApiTableExportOperation(new JsonExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope, engine, resultTypeFileExtensionMapper);
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

        JsonApiTableExportOperation jsonAPIOperation = new JsonApiTableExportOperation(new JsonExportFormatter(elide), asyncExecutorService,
                queryObj, requestScope, engine, resultTypeFileExtensionMapper);
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

        JsonApiTableExportOperation jsonAPIOperation = new JsonApiTableExportOperation(new JsonExportFormatter(elide),
                        asyncExecutorService, queryObj, requestScope, engine, resultTypeFileExtensionMapper);
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
        Route route = Route.builder().apiVersion(NO_VERSION).build();
        RequestScope scope = RequestScope.builder().route(route).dataStoreTransaction(tx).user(user)
                .requestId(UUID.randomUUID()).elideSettings(elide.getElideSettings()).build();
        tx.save(temp, scope);
        tx.commit(scope);
        tx.close();
    }

    @AfterEach
    public void clearDataStore() {
        dataStore.cleanseTestData();
    }
}
