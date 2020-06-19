/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncQueryResultStorage;

import java.util.Collection;
import java.util.UUID;

public interface ResultStorageEngine {

    /**
     * Stores the result of the query in the AsyncQueryResultStorage table and returns the URL to result.
     * @param asyncQueryID is the query ID of the AsyncQuery
     * @param responseBody is the result obtained by running the query
     * @return it returns the URL from where we can access the result of the query
     */
    public AsyncQueryResultStorage storeResults(UUID asyncQueryID, String responseBody);

    /**
     * Searches for the query with ID as AsyncQueryID in the AsyncQueryResultStorage table and returns the record.
     * @param AsyncQueryID is the query ID of the AsyncQuery
     * @return returns the record associated with the AsyncQueryID
     */
    public AsyncQueryResultStorage getResultsByID(String AsyncQueryID);

    /**
     * Deletes the record with ID as AsyncQueryID in the AsyncQueryResultStorage table and returns the record.
     * @param AsyncQueryID AsyncQueryID is the query ID of the AsyncQuery
     * @return returns the record associated with the AsyncQueryID
     */
    public AsyncQueryResultStorage deleteResultsByID(String AsyncQueryID);

    /**
     * Deletes all the records from the AsyncQueryResultStorage table.
     * @return returns the collection of all deleted results
     */
    public Collection<AsyncQueryResultStorage> deleteAllResults();

}
