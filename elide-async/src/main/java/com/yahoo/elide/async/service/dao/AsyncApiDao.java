/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.dao;

import com.yahoo.elide.async.models.AsyncApi;
import com.yahoo.elide.async.models.AsyncApiResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.filter.expression.FilterExpression;

/**
 * Utility interface which uses the elide datastore to modify and create AsyncAPI and AsyncAPIResult Objects.
 */
public interface AsyncApiDao {

    /**
     * This method updates the QueryStatus for AsyncAPI for given QueryStatus.
     * @param asyncAPIId The AsyncAPI Object to be updated
     * @param status Status from Enum QueryStatus
     * @param type AsyncAPI Type Implementation.
     * @return AsyncAPI Updated AsyncAPI Object
     */
    public <T extends AsyncApi> T updateStatus(String asyncAPIId, QueryStatus status, Class<T> type);

    /**
     * This method persists the model for AsyncAPIResult, AsyncAPI object and establishes the relationship.
     * @param asyncAPIResult AsyncAPIResult to be associated with the AsyncAPI object
     * @param asyncAPIId String
     * @param type AsyncAPI Type Implementation.
     * @return AsyncAPI Object
     */
    public <T extends AsyncApi> T updateAsyncApiResult(AsyncApiResult asyncAPIResult,
            String asyncAPIId, Class<T> type);

    /**
     * This method deletes a Iterable of AsyncAPI and its associated AsyncAPIResult objects from database
     * based on a filter expression, and returns the objects deleted.
     * @param filterExpression filter expression to delete AsyncAPI Objects based on
     * @param type AsyncAPI Type Implementation.
     * @return query object Iterable deleted
     */
    public <T extends AsyncApi> Iterable<T> deleteAsyncApiAndResultByFilter(
            FilterExpression filterExpression, Class<T> type);

    /**
     * This method updates the status for a Iterable of AsyncAPI objects from database based on a filter expression, and
     * returns the objects updated.
     * @param  filterExpression filter expression to update AsyncAPI Objects based on
     * @param status status to be updated
     * @param type AsyncAPI Type Implementation.
     * @return query object Iterable updated
     */
    public <T extends AsyncApi> Iterable<T> updateStatusAsyncApiByFilter(FilterExpression filterExpression,
            QueryStatus status, Class<T> type);
    /**
     * This method gets a Iterable of AsyncAPI objects from database and
     * returns the objects.
     * @param filterExpression filter expression to cancel AsyncAPI Objects based on
     * @param type AsyncAPI Type Implementation.
     * @return query object Iterable loaded
     */
    public <T extends AsyncApi> Iterable<T> loadAsyncApiByFilter(FilterExpression filterExpression,
            Class<T> type);
}
