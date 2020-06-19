/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQueryResultStorage;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.request.EntityProjection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.rowset.serial.SerialBlob;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


@Singleton
@Slf4j
@Getter
public class DefaultResultStorageEngine implements ResultStorageEngine {

    @Setter private Elide elide;
    @Setter private DataStore dataStore;
    private EntityDictionary dictionary;
    private RSQLFilterDialect filterParser;

    public DefaultResultStorageEngine() {
    }

    public DefaultResultStorageEngine(Elide elide, DataStore dataStore) {
        this.elide = elide;
        this.dataStore = dataStore;
        dictionary = elide.getElideSettings().getDictionary();
        filterParser = new RSQLFilterDialect(dictionary);
    }

    @Override
    public AsyncQueryResultStorage storeResults(UUID asyncQueryID, String responseBody) {
        byte[] temp = responseBody.getBytes();
        Blob response = null;
        try {
            response = new SerialBlob(temp);
        } catch (SQLException e) {
            log.error("Exception: {}", e);
        }
        AsyncQueryResultStorage asyncQueryResultStorage = new AsyncQueryResultStorage();
        asyncQueryResultStorage.setId(asyncQueryID);
        asyncQueryResultStorage.setResult(response);

        URL url = null;
        try {
            url = new URL("https://elide.io/asyncQueryID");
        } catch (MalformedURLException e) {
            log.error("Exception: {}", e);
        }
        return asyncQueryResultStorage;

    }

    @Override
    public AsyncQueryResultStorage getResultsByID(String asyncQueryID) {
        log.debug("deleteAllResults");

        AsyncQueryResultStorage asyncQueryResultStorages = null;

        try {
            asyncQueryResultStorages = (AsyncQueryResultStorage) executeInTransaction(dataStore, (tx, scope) -> {

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQueryResultStorage.class)
                        .build();

                Object loaded = tx.loadObject(asyncQueryCollection, asyncQueryID, scope);

                return loaded;


            });
        } catch (Exception e) {
            log.error("Exception: {}", e);
        }
        return asyncQueryResultStorages;
    }

    @Override
    public AsyncQueryResultStorage deleteResultsByID(String asyncQueryID) {
        log.debug("deleteAllResults");

        AsyncQueryResultStorage asyncQueryResultStorages = null;

        try {
            asyncQueryResultStorages = (AsyncQueryResultStorage) executeInTransaction(dataStore, (tx, scope) -> {

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQueryResultStorage.class)
                        .build();

                Object loaded = tx.loadObject(asyncQueryCollection, asyncQueryID, scope);
                Object result = loaded;

                if (result != null) {
                    tx.delete(result, scope);
                }

                return loaded;


            });
        } catch (Exception e) {
            log.error("Exception: {}", e);
        }
        return asyncQueryResultStorages;

    }

    @Override
    public Collection<AsyncQueryResultStorage> deleteAllResults() {
        log.debug("deleteAllResults");

        Collection<AsyncQueryResultStorage> asyncQueryResultStorages = null;

        try {
            asyncQueryResultStorages = (Collection<AsyncQueryResultStorage>) executeInTransaction(dataStore,
                    (tx, scope) -> {

                        EntityProjection asyncQueryCollection = EntityProjection.builder()
                                .type(AsyncQueryResultStorage.class)
                                .build();

                        Iterable<Object> loaded = tx.loadObjects(asyncQueryCollection, scope);
                        Iterator<Object> itr = loaded.iterator();

                        while (itr.hasNext()) {
                            AsyncQueryResultStorage query = (AsyncQueryResultStorage) itr.next();
                            if (query != null) {
                                tx.delete(query, scope);
                            }
                        }
                        return loaded;
                    });
        } catch (Exception e) {
            log.error("Exception: {}", e);
        }
        return asyncQueryResultStorages;

    }

    /**
     * This method creates a transaction from the datastore, performs the DB action using
     * a generic functional interface and closes the transaction.
     * @param dataStore Elide datastore retrieved from Elide object
     * @param action Functional interface to perform DB action
     * @return Object Returns Entity Object (AsyncQueryResult or AsyncResult)
     */
    protected Object executeInTransaction(DataStore dataStore, Transactional action) {
        log.debug("executeInTransaction");
        Object result = null;
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
            RequestScope scope = new RequestScope("query", NO_VERSION, jsonApiDoc,
                    tx, null, queryParams, elide.getElideSettings());
            result = action.execute(tx, scope);
            tx.flush(scope);
            tx.commit(scope);
        } catch (IOException e) {
            log.error("IOException: {}", e);
        } catch (Exception e) {
            log.error("Exception: {}", e);
        }
        return result;
    }
}
