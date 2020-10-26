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
 * Utility interface which uses the elide datastore to modify and create AsyncAPI and AsyncAPIResult Objects.
 */
public interface AsyncAPIDAO {

    /**
     * This method updates the QueryStatus for AsyncAPI for given QueryStatus.
     * @param asyncAPIId The AsyncAPI Object to be updated
     * @param status Status from Enum QueryStatus
     * @param type AsyncAPI Type Implementation.
     * @return AsyncAPI Updated AsyncAPI Object
     */
    public <T extends AsyncAPI> T updateStatus(String asyncAPIId, QueryStatus status, Class<T> type);

    /**
     * This method persists the model for AsyncAPIResult, AsyncAPI object and establishes the relationship.
     * @param asyncAPIResult AsyncAPIResult to be associated with the AsyncAPI object
     * @param asyncAPIId String
     * @param type AsyncAPI Type Implementation.
     * @return AsyncAPI Object
     */
    public <T extends AsyncAPI> T updateAsyncAPIResult(AsyncAPIResult asyncAPIResult,
            String asyncAPIId, Class<T> type);

    /**
     * This method deletes a collection of AsyncAPI and its associated AsyncAPIResult objects from database and
     * returns the objects deleted.
     * @param filterExpression filter expression to delete AsyncAPI Objects based on
     * @param type AsyncAPI Type Implementation.
     * @return query object list deleted
     */
    public <T extends AsyncAPI> Collection<T> deleteAsyncAPIAndResultCollection(
            FilterExpression filterExpression, Class<T> type);

    /**
     * This method updates the status for a collection of AsyncAPI objects from database and
     * returns the objects updated.
     * @param  filterExpression filter expression to update AsyncAPI Objects based on
     * @param status status to be updated
     * @param type AsyncAPI Type Implementation.
     * @return query object list updated
     */
    public <T extends AsyncAPI> Collection<T> updateStatusAsyncAPICollection(FilterExpression filterExpression,
            QueryStatus status, Class<T> type);
    /**
     * This method gets a collection of AsyncAPI objects from database and
     * returns the objects.
     * @param filterExpression filter expression to cancel AsyncAPI Objects based on
     * @param type AsyncAPI Type Implementation.
     * @return query object list updated
     */
    public <T extends AsyncAPI> Collection<T> loadAsyncAPICollection(FilterExpression filterExpression,
            Class<T> type);
}
