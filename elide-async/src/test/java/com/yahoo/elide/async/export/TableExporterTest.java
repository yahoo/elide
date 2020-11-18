/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.security.AsyncQueryInlineChecks;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.Check;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

public class TableExporterTest {

    private HashMapDataStore dataStore;
    private Elide elide;
    private User user;

    @BeforeEach
    public void beforeTest() {
        dataStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> map = new HashMap<>();
        map.put(AsyncQueryInlineChecks.AsyncQueryOwner.PRINCIPAL_IS_OWNER,
                AsyncQueryInlineChecks.AsyncQueryOwner.class);
        map.put(AsyncQueryInlineChecks.AsyncQueryAdmin.PRINCIPAL_IS_ADMIN,
                AsyncQueryInlineChecks.AsyncQueryAdmin.class);
        map.put(AsyncQueryInlineChecks.AsyncQueryStatusValue.VALUE_IS_CANCELLED,
                AsyncQueryInlineChecks.AsyncQueryStatusValue.class);
        map.put(AsyncQueryInlineChecks.AsyncQueryStatusQueuedValue.VALUE_IS_QUEUED,
                AsyncQueryInlineChecks.AsyncQueryStatusQueuedValue.class);
        elide = new Elide(
                    new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(new EntityDictionary(map))
                        .withAuditLogger(new Slf4jLogger())
                        .build());
        user = mock(User.class);
    }

    @Test
    public void testExporterNonEmptyProjection() throws IOException {
        dataPrep();
        // Query
        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setQueryType(QueryType.GRAPHQL_V1_0);
        asyncQuery.setQuery("{\"query\":\"{ asyncQuery { edges { node { id principalName} } } }\",\"variables\":null}");

        TableExporter exporter = new TableExporter(elide, NO_VERSION, user);
        Observable<PersistentResource> results = exporter.export(asyncQuery);

        assertNotEquals(Observable.empty(), results);
        assertEquals(1, results.toList(LinkedHashSet::new).blockingGet().size());
    }

    @Test
    public void testExporterJsonAPI() {
        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setQueryType(QueryType.JSONAPI_V1_0);

        TableExporter exporter = new TableExporter(elide, NO_VERSION, user);
        assertThrows(InvalidValueException.class, () -> exporter.export(asyncQuery));
    }

    /**
     * Prepping and Storing an asyncQuery entry to be queried later on.
     * @throws IOException  IOException
     */
    private void dataPrep() throws IOException {
        AsyncQuery temp = new AsyncQuery();
        DataStoreTransaction tx = dataStore.beginTransaction();
        RequestScope scope = new RequestScope(null, null, NO_VERSION, null, tx, user, null, UUID.randomUUID(), elide.getElideSettings());
        tx.save(temp, scope);
        tx.commit(scope);
        tx.close();
    }

    @AfterEach
    public void clearDataStore() {
        dataStore.cleanseTestData();
    }
}
