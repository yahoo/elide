/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.models.AsyncQuery;

import io.reactivex.Observable;

/**
 * Utility interface used for storing the results of AsyncQuery for downloads.
 */
public interface ResultStorageEngine {

    /**
     * Stores the result of the query.
     * @param asyncQuery AsyncQuery object
     * @param result is the observable result obtained by running the query
     * @return String to store as attachment. Can be null.
     */
    public AsyncQuery storeResults(AsyncQuery asyncQuery, Observable<String> result);

    /**
     * Searches for the async query results by ID and returns the record.
     * @param asyncQueryID is the query ID of the AsyncQuery
     * @return returns the result associated with the AsyncQueryID
     */
    public Observable<String> getResultsByID(String asyncQueryID);
}
