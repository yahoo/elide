/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

import com.yahoo.elide.async.export.TableExporter;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.PersistentResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.opendevl.JFlat;

import org.apache.http.NoHttpResponseException;

import io.reactivex.Observable;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * TableExport implementation of Callable for executing the query provided in TableExport.
 * It will also update the query status and result object at different stages of execution.
 */
@Slf4j
public class TableExportThread implements Callable<AsyncAPIResult> {
    private TableExport queryObj;
    private TableExporter exporter;
    private Integer downloadRecordCount = 0;
    private ResultStorageEngine resultStorageEngine;
    private ObjectMapper mapper = new ObjectMapper();

    public TableExportThread(TableExport queryObj, ResultStorageEngine resultStorageEngine, TableExporter exporter) {
        this.queryObj = queryObj;
        this.exporter = exporter;
        this.resultStorageEngine = resultStorageEngine;
    }

    @Override
    public AsyncAPIResult call() throws URISyntaxException, NoHttpResponseException, MalformedURLException {
        Observable<PersistentResource> observableResults = exporter.export(queryObj);
        Observable<String> downloadString = processObservablePersistentResource(observableResults);
        storeResults(downloadString);

        TableExportResult queryResult = new TableExportResult();
        queryResult.setHttpStatus(200);
        queryResult.setCompletedOn(new Date());
        //TODO Add URL generation logic and set
        //queryResult.setUrl(new URL("url"));
        queryResult.setRecordCount(downloadRecordCount);
        return queryResult;
    }

    /**
     * This method stores the results.
     * @param result is the observable result to store.
     * @return TableExport object
     */
    protected TableExport storeResults(Observable<String> result) {
        return resultStorageEngine.storeResults(queryObj, result);
    }

    /**
     * Process Observable of Persistent Resource to generate the Observable of download-ready result string.
     * @param resources Observable Persistent Resource.
     * @return result as Observable of String
     */
    protected Observable<String> processObservablePersistentResource(Observable<PersistentResource> resources) {
        Observable<String> results = Observable.just("No Records Generated");

        if (queryObj.getResultType() == ResultType.CSV) {
            results =  resources.map(resource -> convertToCSV(resource));
        } else if (queryObj.getResultType() == ResultType.JSON) {
            results = resources.map(resource -> resourceToJsonStr(resource));
        }
        return results;
    }

    private String resourceToJsonStr(PersistentResource resource) throws IOException {
        incrementRecordCount();
        //TODO Additional logic to create an array of records???
        return resource == null ? null : mapper.writeValueAsString(resource.getObject());
    }

    /**
     * This method converts the JSON response to a CSV response type.
     * @param resource is the Persistent Resource to convert
     * @return returns string which is in CSV format
     * @throws IllegalStateException Exception thrown
     */
    protected String convertToCSV(PersistentResource resource) {
        if (resource == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();

        List<Object[]> json2Csv;

        try {
            String jsonStr = resourceToJsonStr(resource);

            JFlat flat = new JFlat(jsonStr);

            json2Csv = flat.json2Sheet().headerSeparator("_").getJsonAsSheet();

            int index = 0;

            for (Object[] obj : json2Csv) {
                // Skip Header record from 2nd time onwards.
                if (index++ == 0 && downloadRecordCount != 0) {
                    continue;
                }

                String objString = Arrays.toString(obj);
                if (objString != null) {
                    objString = objString.substring(1, objString.length() - 1);
                }
                str.append(objString);
                // Only append new lines after header. Other records have a new line present already.
                if (index == 1) {
                    str.append(System.getProperty("line.separator"));
                }

                incrementRecordCount();
            }
        } catch (Exception e) {
            log.debug("Exception while converting to CSV: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        return str.toString();
    }

    /**
     * Increment downloadable record count.
     */
    protected void incrementRecordCount() {
        this.downloadRecordCount++;
    }
}
