/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncQueryResult;

import io.reactivex.Observable;

/**
 * Utility interface used for storing the results of AsyncQuery for downloads.
 */
public interface ResultStorageEngine {

    /**
     * Stores the result of the query.
     * @param asyncQueryResult AsyncQueryResult for storing the results
     * @param result is the result obtained by running the query
     * @param asyncQueryId is the ID of the query
     * @return AsyncQueryResult object
     */
    public AsyncQueryResult storeResults(AsyncQueryResult asyncQueryResult, String result, String asyncQueryId);

    /**
     * Searches for the async query results by ID and returns the record.
     * @param asyncQueryID is the query ID of the AsyncQuery
     * @return returns the result associated with the AsyncQueryID
     */
    public Observable<String> getResultsByID(String asyncQueryID);
}
