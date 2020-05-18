/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;

import java.util.Collection;

/**
 * Utility interface which uses the elide datastore to modify and create AsyncQuery and AsyncQueryResult Objects.
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
     * This method persists the model for AsyncQueryResult, AsyncQuery object and establishes the relationship.
     * @param status ElideResponse status from AsyncQuery
     * @param responseBody ElideResponse responseBody from AsyncQuery
     * @param asyncQuery AsyncQuery object to be associated with the AsyncQueryResult object
     * @param asyncQueryId UUID of the AsyncQuery to be associated with the AsyncQueryResult object
     * @return AsyncQueryResult Object
     */
    public AsyncQueryResult createAsyncQueryResult(Integer status, String responseBody, AsyncQuery asyncQuery,
            String asyncQueryId);

    /**
     * This method deletes a collection of AsyncQuery and its associated AsyncQueryResult objects from database and
     * returns the objects deleted.
     * @param filterExpression filter expression to delete AsyncQuery Objects based on
     * @return query object list deleted
     */
    public Collection<AsyncQuery> deleteAsyncQueryAndResultCollection(String filterExpression);

    /**
     * This method updates the status for a collection of AsyncQuery objects from database and
     * returns the objects updated.
     * @param  filterExpression filter expression to update AsyncQuery Objects based on
     * @param status status to be updated
     * @return query object list updated
     */
    public Collection<AsyncQuery> updateStatusAsyncQueryCollection(String filterExpression,
            QueryStatus status);

}
