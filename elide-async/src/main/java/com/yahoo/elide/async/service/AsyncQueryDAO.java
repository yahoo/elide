/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.util.UUID;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.core.DataStore;

/**
 * Utility interface which uses the elide datastore to modify, update and create
 * AsyncQuery and AsyncQueryResult Objects
 */
public interface AsyncQueryDAO {

    /**
     * Set elide object
     * @param elide Elide Object.
     */
    public void setElide(Elide elide);

    /**
     * Set data store object
     * @param dataStore Datastore Object from Elide.
     */
    public void setDataStore(DataStore dataStore);

    /**
     * This method updates the model for AsyncQuery with passed value.
     * @param asyncQueryId Unique UUID for the AsyncQuery Object
     * @param updateFunction Functional interface for updating AsyncQuery Object
     * @return AsyncQuery Object
     */
    public AsyncQuery updateAsyncQuery(UUID asyncQueryId, UpdateQuery updateFunction);

    /**
     * This method updates a collection of AsyncQuery objects from database and
     * returns the objects updated.
     * @param asyncQueryList Iterable list of AsyncQuery objects to be updated
     * @return query object list updated
     */
    public Iterable<Object> updateAsyncQueryCollection(Iterable<Object> asyncQueryList, UpdateQuery updateFunction);

    /**
     * This method deletes a collection of AsyncQuery and AsyncQueryResult objects from database and
     * returns the objects deleted.
     * @param asyncQueryList Iterable list of AsyncQuery objects to be deleted
     * @return query object list deleted
     */
    public Iterable<Object> deleteAsyncQueryAndResultCollection(Iterable<Object> asyncQueryList);

    /**
     * This method persists the model for AsyncQueryResult, AsyncQuery object and establishes the relationship
     * @param status ElideResponse status from AsyncQuery
     * @param responseBody ElideResponse responseBody from AsyncQuery
     * @param asyncQuery AsyncQuery object to be associated with the AsyncQueryResult object
     * @param asyncQueryId UUID of the AsyncQuery to be associated with the AsyncQueryResult object
     * @return AsyncQueryResult Object
     */
    public AsyncQueryResult setAsyncQueryAndResult(Integer status, String responseBody, AsyncQuery asyncQuery, UUID asyncQueryId);

    /**
     * This method creates a transaction from the datastore, performs the DB action using
     * a generic functional interface and closes the transaction.
     * @param dataStore Elide datastore retrieved from Elide object
     * @param action Functional interface to perform DB action
     * @return Object Returns Entity Object (AsyncQueryResult or AsyncResult)
     */
    public Object executeInTransaction(DataStore dataStore, Transactional action);

}
