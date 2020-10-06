/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.security.User;

import io.reactivex.Observable;

public class TableExporterTest {

    private DataStoreTransaction tx = mock(DataStoreTransaction.class);
    private Elide elide;
    private User user;
    private GraphQLParser graphQLParser;
    private AsyncQuery asyncQuery;

    @BeforeEach
    public void beforeTest() {
        reset(tx);
        elide = mock(Elide.class);
        user = mock(User.class);
        asyncQuery = mock(AsyncQuery.class);
        graphQLParser = mock(GraphQLParser.class);

        TransactionRegistry transactionRegistry = mock(TransactionRegistry.class);
        DataStore dataStore = mock(DataStore.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        ElideSettings settings = mock(ElideSettings.class);
        EntityDictionary dictionary = mock(EntityDictionary.class);
        EntityProjection projection = EntityProjection.builder().type(AsyncQuery.class).build();

        when(asyncQuery.getRequestId()).thenReturn(UUID.randomUUID().toString());
        when(elide.getTransactionRegistry()).thenReturn(transactionRegistry);
        when(elide.getAuditLogger()).thenReturn(auditLogger);
        when(elide.getDataStore()).thenReturn(dataStore);
        when(elide.getElideSettings()).thenReturn(settings);
        when(settings.getDictionary()).thenReturn(dictionary);
        when(dataStore.beginTransaction()).thenReturn(tx);
        when(graphQLParser.parse(any())).thenReturn(projection);
    }
    
    @Test
    public void testExporterEmptyProjection() {
        TableExporter exporter = new TableExporter(elide, NO_VERSION, user, graphQLParser);
        Object[] queries= {asyncQuery};
        when(tx.loadObjects(any(), any())).thenReturn(Arrays.asList(queries));

        Observable<PersistentResource> results = exporter.export(asyncQuery);
        
        assertNotEquals(Observable.empty(), results);
        assertEquals(1, results.toList(LinkedHashSet::new).blockingGet().size());
    }
}
