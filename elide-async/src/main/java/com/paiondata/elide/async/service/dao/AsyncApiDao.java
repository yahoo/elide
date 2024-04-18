/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.dao;

import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.core.filter.expression.FilterExpression;

/**
 * Utility interface which uses the elide datastore to modify and create AsyncApi and AsyncApiResult Objects.
 */
public interface AsyncApiDao {

    /**
     * This method updates the QueryStatus for AsyncApi for given QueryStatus.
     * @param asyncApiId The AsyncApi Object to be updated
     * @param status Status from Enum QueryStatus
     * @param type AsyncApi Type Implementation.
     * @return AsyncApi Updated AsyncApi Object
     */
    public <T extends AsyncApi> T updateStatus(String asyncApiId, QueryStatus status, Class<T> type);

    /**
     * This method persists the model for AsyncApiResult, AsyncApi object and establishes the relationship.
     * @param asyncApiResult AsyncApiResult to be associated with the AsyncApi object
     * @param asyncApiId String
     * @param type AsyncApi Type Implementation.
     * @return AsyncApi Object
     */
    public <T extends AsyncApi> T updateAsyncApiResult(AsyncApiResult asyncApiResult,
            String asyncApiId, Class<T> type);

    /**
     * This method deletes a Iterable of AsyncApi and its associated AsyncApiResult objects from database
     * based on a filter expression, and returns the objects deleted.
     * @param filterExpression filter expression to delete AsyncApi Objects based on
     * @param type AsyncApi Type Implementation.
     * @return query object Iterable deleted
     */
    public <T extends AsyncApi> Iterable<T> deleteAsyncApiAndResultByFilter(
            FilterExpression filterExpression, Class<T> type);

    /**
     * This method updates the status for a Iterable of AsyncApi objects from database based on a filter expression, and
     * returns the objects updated.
     * @param  filterExpression filter expression to update AsyncApi Objects based on
     * @param status status to be updated
     * @param type AsyncApi Type Implementation.
     * @return query object Iterable updated
     */
    public <T extends AsyncApi> Iterable<T> updateStatusAsyncApiByFilter(FilterExpression filterExpression,
            QueryStatus status, Class<T> type);
    /**
     * This method gets a Iterable of AsyncApi objects from database and
     * returns the objects.
     * @param filterExpression filter expression to cancel AsyncApi Objects based on
     * @param type AsyncApi Type Implementation.
     * @return query object Iterable loaded
     */
    public <T extends AsyncApi> Iterable<T> loadAsyncApiByFilter(FilterExpression filterExpression,
            Class<T> type);
}
