/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

import com.yahoo.elide.async.export.TableExportParser;
import com.yahoo.elide.async.export.TableExporter;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.opendevl.JFlat;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.utils.URIBuilder;

import io.reactivex.Observable;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * TableExport implementation of Callable for executing the query provided in TableExport.
 * It will also update the query status and result object at different stages of execution.
 */
@Slf4j
public class TableExportThread implements Callable<AsyncAPIResult> {
    private static final String FORWARD_SLASH = "/";
    private static final String COMMA = ",";
    private static final String DOUBLE_QUOTES = "\"";

    private TableExport queryObj;
    private TableExporter exporter;
    private Integer downloadRecordCount = 0;
    private ResultStorageEngine resultStorageEngine;
    private ObjectMapper mapper = new ObjectMapper();
    private String baseURL;
    private String downloadURI;
    private boolean skipCSVHeader;
    private String apiVersion;
    private User user;

    public TableExportThread(TableExport queryObj, TableExporter exporter,
            String baseURL, String apiVersion, User user,
            ResultStorageEngine resultStorageEngine, String downloadURI, boolean skipCSVHeader) {
        this.queryObj = queryObj;
        this.exporter = exporter;
        this.resultStorageEngine = resultStorageEngine;
        this.baseURL = baseURL;
        this.downloadURI = downloadURI;
        this.skipCSVHeader = skipCSVHeader;
        this.apiVersion = apiVersion;
        this.user = user;
    }

    @Override
    public AsyncAPIResult call() throws URISyntaxException, NoHttpResponseException, MalformedURLException {
        Observable<PersistentResource> observableResults = exporter.export(queryObj, user, apiVersion);
        Observable<String> downloadString = processObservablePersistentResource(observableResults);
        // Generate Header for CSV even when no records generated
        if (downloadRecordCount == 0 && !skipCSVHeader && queryObj.getResultType() == ResultType.CSV) {
            // TODO Test if this method can be used when data is present too,
            // instead of relying on Json2Flat Header generation.
            downloadString = generateCSVHeader(exporter.getParser(queryObj));
        }
        storeResults(downloadString);

        TableExportResult queryResult = new TableExportResult();
        queryResult.setHttpStatus(200);
        queryResult.setCompletedOn(new Date());
        queryResult.setUrl(generateDownloadURL());
        queryResult.setRecordCount(downloadRecordCount);
        return queryResult;
    }

    private String getBasePath(String URL) {
        URIBuilder uri;
        try {
            uri = new URIBuilder(URL);
        } catch (URISyntaxException e) {
            log.debug("extracting base path from requestURL failure. {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        StringBuilder str = new StringBuilder(uri.getScheme() + "://" + uri.getHost());
        if (uri.getPort() != -1) {
            str.append(":" + uri.getPort());
        }
        return str.toString();
    }

    private URL generateDownloadURL() {
        log.debug("generateDownloadUrl");
        String asyncQueryID = queryObj.getId();
        String basePath = baseURL != null ? getBasePath(baseURL) : null;
        String tempURL = basePath != null && downloadURI != null ? basePath + downloadURI : null;

        String urlString = null;
        if (tempURL != null) {
            urlString = tempURL.endsWith(FORWARD_SLASH) ? tempURL + asyncQueryID
                    : tempURL + FORWARD_SLASH + asyncQueryID;
        }

        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            log.error("Results stored, unable to generate URL. Exception: {}", e);
            throw new IllegalStateException(e);
        }
        return url;
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
        Observable<String> results = Observable.empty();

        if (queryObj.getResultType() == ResultType.CSV) {
            results =  resources.map(resource -> {
                incrementRecordCount();
                return convertToCSV(resource);
            });
        } else if (queryObj.getResultType() == ResultType.JSON) {
            // Add "[" for start of json array;
            results = Observable.just("[");
            results.concatWith(resources.map(resource -> {
                incrementRecordCount();
                return resourceToJsonStr(resource);
            }));
            // Add "]" for end of json array;
            results.concatWith(Observable.just("]"));
        }
        return results;
    }

    protected String resourceToJsonStr(PersistentResource resource) throws IOException {
        if (resource == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();
        if (this.downloadRecordCount > 1) {
            // Add "," to separate individual json rows within the array
            str.append(COMMA);
        }

        str.append(mapper.writeValueAsString(resource.getObject()));
        return str.toString();
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
                // convertToCSV is called once for each PersistentResource in the observable.
                // json2Csv will always have 2 entries.
                // 0th index is the header so we need to skip the header from 2nd time this method is called.
                // (index++ == 0 && downloadRecordCount != 1) is evaluated first as || is Left associative.
                if (index++ == 0 && (downloadRecordCount != 1 || skipCSVHeader)) {
                    continue;
                }

                String objString = Arrays.toString(obj);
                if (objString != null) {
                    //The arrays.toString returns o/p with [ and ] at the beginning and end. So need to exclude them.
                    objString = objString.substring(1, objString.length() - 1);
                }
                str.append(objString);
                // Only append new lines after header. Cause 1st resource is transformed to 2 lines.
                if (index == 1 && !skipCSVHeader) {
                    str.append(System.getProperty("line.separator"));
                }
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

    /**
     * Generate CSV Header when Observable is Empty.
     * @param parser TableExportParser object.
     * @return returns Header string which is in CSV format.
     */
    protected Observable<String> generateCSVHeader(TableExportParser parser) {
        EntityProjection projection = parser.parse(queryObj, apiVersion);

        Observable<String> header = Observable.empty();
        Iterator itr = projection.getAttributes().iterator();
        StringBuilder str = new StringBuilder();
        int columnCount = 0;
        while (itr.hasNext()) {
            if (columnCount > 0) {
                // Add "," to separate column from 2nd column onwards.
                str.append(COMMA);
            }
            // Append DoubleQuotes around column names.
            str.append(DOUBLE_QUOTES);
            Attribute atr = (Attribute) itr.next();
            String alias = atr.getAlias();
            str.append(alias != null && !alias.isEmpty() ? alias : atr.getName());
            str.append(DOUBLE_QUOTES);
            columnCount++;
        }
        header = Observable.just(str.toString());
        return header;
    }
}
