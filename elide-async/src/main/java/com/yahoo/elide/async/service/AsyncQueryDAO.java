/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.filter.expression.FilterExpression;

import java.util.Collection;

/**
 * Utility interface which uses the elide datastore to modify and create AsyncQuery and AsyncQueryResult Objects.
 */
public interface AsyncQueryDAO {

    /**
     * This method updates the QueryStatus for AsyncQuery for given QueryStatus.
     * @param asyncQueryId The AsyncQuery Object to be updated
     * @param status Status from Enum QueryStatus
     * @param type AsyncAPI Type Implementation.
     * @return AsyncQuery Updated AsyncQuery Object
     */
    public <T extends AsyncAPI> T updateStatus(String asyncQueryId, QueryStatus status, Class<T> type);

    /**
     * This method persists the model for AsyncQueryResult, AsyncQuery object and establishes the relationship.
     * @param asyncQueryResult AsyncQueryResult to be associated with the AsyncQuery object
     * @param asyncQueryId String
     * @param type AsyncAPI Type Implementation.
     * @return AsyncQuery Object
     */
    public <T extends AsyncAPI> T updateAsyncQueryResult(AsyncAPIResult asyncQueryResult,
            String asyncQueryId, Class<T> type);

    /**
     * This method deletes a collection of AsyncQuery and its associated AsyncQueryResult objects from database and
     * returns the objects deleted.
     * @param filterExpression filter expression to delete AsyncQuery Objects based on
     * @param type AsyncAPI Type Implementation.
     * @return query object list deleted
     */
    public <T extends AsyncAPI> Collection<T> deleteAsyncQueryAndResultCollection(
            FilterExpression filterExpression, Class<T> type);

    /**
     * This method updates the status for a collection of AsyncQuery objects from database and
     * returns the objects updated.
     * @param  filterExpression filter expression to update AsyncQuery Objects based on
     * @param status status to be updated
     * @param type AsyncAPI Type Implementation.
     * @return query object list updated
     */
    public <T extends AsyncAPI> Collection<T> updateStatusAsyncQueryCollection(FilterExpression filterExpression,
            QueryStatus status, Class<T> type);
    /**
     * This method gets a collection of AsyncQuery objects from database and
     * returns the objects.
     * @param filterExpression filter expression to cancel AsyncQuery Objects based on
     * @param type AsyncAPI Type Implementation.
     * @return query object list updated
     */
    public <T extends AsyncAPI> Collection<T> loadAsyncQueryCollection(FilterExpression filterExpression,
            Class<T> type);
}
