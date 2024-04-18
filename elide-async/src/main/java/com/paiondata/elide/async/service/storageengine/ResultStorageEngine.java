/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.async.service.storageengine;

import com.paiondata.elide.async.models.TableExportResult;

import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * Utility interface used for storing the results of AsyncQuery for downloads.
 */
public interface ResultStorageEngine {
    public static final String RETRIEVE_ERROR = "Unable to retrieve results.";
    public static final String STORE_ERROR = "Unable to store results.";

    /**
     * Stores the result of the query.
     * @param tableExportID is the ID of the TableExport. It may include extension too if enabled.
     * @param result is the observable result obtained by running the query
     * @return TableExportResult.
     */
    public TableExportResult storeResults(String tableExportID, Consumer<OutputStream> result);

    /**
     * Searches for the async query results by ID and returns the record.
     * @param tableExportID is the ID of the TableExport. It may include extension too if enabled.
     * @return returns the result associated with the tableExportID
     */
    public Consumer<OutputStream> getResultsByID(String tableExportID);
}
