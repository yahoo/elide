/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResultStorage;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.request.EntityProjection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import javax.inject.Singleton;
import javax.sql.rowset.serial.SerialBlob;

/**
 * Utility class which implements ResultStorageEngine.
 */
@Singleton
@Slf4j
@Getter
public class DefaultResultStorageEngine implements ResultStorageEngine {

    @Setter private Elide elide;
    @Setter private DataStore dataStore;
    private String baseURL;

    public DefaultResultStorageEngine() {
    }

    public DefaultResultStorageEngine(Elide elide, DataStore dataStore, String baseURL) {
        this.elide = elide;
        this.dataStore = dataStore;
        this.baseURL = baseURL;
    }

    @Override
    public URL storeResults(String asyncQueryID, byte[] byteResponse) {

        AsyncQueryResultStorage asyncQueryResultStorage1 = null;
        try {
            Blob response = new SerialBlob(byteResponse);

            asyncQueryResultStorage1 = (AsyncQueryResultStorage) DBUtil.executeInTransaction(elide, dataStore,
                    (tx, scope) -> {

                        AsyncQueryResultStorage asyncQueryResultStorage = new AsyncQueryResultStorage();
                        asyncQueryResultStorage.setId(asyncQueryID);
                        asyncQueryResultStorage.setResult(response);

                        tx.save(asyncQueryResultStorage, scope);

                        return asyncQueryResultStorage;
                    });
        } catch (SQLException e) {
            log.error("Exception: {}", e);
            throw new IllegalStateException(e);
        } catch (Exception e) {
            log.error("Exception: {}", e);
            //throw new IllegalStateException(e);
        }

        URL url = null;
        String buildURL = baseURL + "/AsyncQueryResultStorage/" + asyncQueryID;
        try {
            url = new URL(buildURL);
        } catch (MalformedURLException e) {
            log.error("Exception: {}", e);
            throw new IllegalStateException(e);
        }
        return url;

    }

    @Override
    public byte[] getResultsByID(String asyncQueryID) {
        log.debug("getResultsByID");

        AsyncQueryResultStorage asyncQueryResultStorage = null;
        byte[] byteResult = null;

        try {
            asyncQueryResultStorage = (AsyncQueryResultStorage) DBUtil.executeInTransaction(elide,
                    dataStore, (tx, scope) -> {

                        EntityProjection asyncQueryCollection = EntityProjection.builder()
                                .type(AsyncQueryResultStorage.class)
                                .build();

                        Object loaded = tx.loadObject(asyncQueryCollection, asyncQueryID, scope);

                        return loaded;
                    });
            if (asyncQueryResultStorage != null) {
                Blob result = asyncQueryResultStorage.getResult();
                byteResult = result.getBytes(1, (int) result.length());
            }
        } catch (SQLException e) {
            log.error("Exception: {}", e);
            throw new IllegalStateException(e);
        }

        return byteResult;
    }

    @Override
    public void deleteResultsCollection(Collection<AsyncQuery> asyncQueryList) {
        log.debug("deleteResultsCollection");

        Collection<AsyncQueryResultStorage> asyncQueryResultStorage = null;
        Iterator<AsyncQuery> itr = asyncQueryList.iterator();
        while (itr.hasNext()) {
            AsyncQuery query = itr.next();

            asyncQueryResultStorage = (Collection<AsyncQueryResultStorage>) DBUtil.executeInTransaction(elide,
                    dataStore, (tx, scope) -> {

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQueryResultStorage.class)
                        .build();

                Object loaded = null;
                loaded = tx.loadObject(asyncQueryCollection, query.getId(), scope);

                if (loaded != null) {
                    tx.delete(loaded, scope);
                }

                return loaded;
            });

        }

    }
}
