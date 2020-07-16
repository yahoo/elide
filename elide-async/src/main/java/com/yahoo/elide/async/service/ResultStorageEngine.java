/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncQuery;

import java.net.URL;
import java.util.Collection;

/**
 * Utility interface which uses the elide datastore to modify and create AsyncQueryResultStorage Object.
 */
public interface ResultStorageEngine {

    /**
     * Stores the result of the query in the AsyncQueryResultStorage table and returns the URL to result.
     * @param asyncQueryID is the query ID of the AsyncQuery
     * @param byteResponse is the result obtained by running the query
     * @return it returns the URL from where we can access the result of the query
     */
    public URL storeResults(String asyncQueryID, byte[] byteResponse);

    /**
     * Searches for the query with ID as AsyncQueryID in the AsyncQueryResultStorage table and returns the record.
     * @param asyncQueryID is the query ID of the AsyncQuery
     * @return returns the record associated with the AsyncQueryID
     */
    public byte[] getResultsByID(String asyncQueryID);

    /**
     * Deletes all the records from the AsyncQuery Collection .
     */
    public void deleteResultsCollection(Collection<AsyncQuery> asyncQueryCollection);

}
