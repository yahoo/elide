/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.util.Collection;
import java.util.UUID;

import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;

/**
 * Utility interface which uses the elide datastore to modify, update and create
 * AsyncQuery and AsyncQueryResult Objects
 */
public interface AsyncQueryDAO {

    /**
     * This method updates the QueryStatus for AsyncQuery for given QueryStatus.
     * @param asyncQuery The AsyncQuery Object to be updated
     * @param status Status from Enum QueryStatus
     * @return AsyncQuery Updated AsyncQuery Object
     */
    public AsyncQuery updateStatus(AsyncQuery asyncQuery, QueryStatus status);

    /**
     * This method uses the filter expression to evaluate a list of filtered results based on the expression
     * and returns a collection of filtered AsyncQuery objects.
     * @param filterExpression filter expression for filtering from datastore
     * @return filtered results
     */
    public Collection<AsyncQuery> loadQueries(String filterExpression);

    /**
     * This method persists the model for AsyncQueryResult, AsyncQuery object and establishes the relationship
     * @param status ElideResponse status from AsyncQuery
     * @param responseBody ElideResponse responseBody from AsyncQuery
     * @param asyncQuery AsyncQuery object to be associated with the AsyncQueryResult object
     * @param asyncQueryId UUID of the AsyncQuery to be associated with the AsyncQueryResult object
     * @return AsyncQueryResult Object
     */
    public AsyncQueryResult createAsyncQueryResult(Integer status, String responseBody, AsyncQuery asyncQuery, UUID asyncQueryId);

    /**
     * This method deletes a collection of AsyncQuery and its associated AsyncQueryResult objects from database and
     * returns the objects deleted.
     * @param asyncQueryList Iterable list of AsyncQuery objects to be deleted
     * @return query object list deleted
     */
    public Collection<AsyncQuery> deleteAsyncQueryAndResultCollection(Collection<AsyncQuery> asyncQueryList);

    /**
     * This method updates the status for a collection of AsyncQuery objects from database and
     * returns the objects updated.
     * @param asyncQueryList Iterable list of AsyncQuery objects to be updated
     * @return query object list updated
     */
    public Collection<AsyncQuery> updateStatusAsyncQueryCollection(Collection<AsyncQuery> asyncQueryList, QueryStatus status);

}
