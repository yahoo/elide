/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import org.apache.http.client.utils.URIBuilder;

import io.reactivex.Observable;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Download Supporting version of AbstractAsyncQueryThread.
 */
@Slf4j
@Data
public class AsyncQueryThread extends AbstractAsyncQueryThread {
    private static final String FORWARD_SLASH = "/";

    private String downloadURI;
    private ResultStorageEngine resultStorageEngine;
    private String requestURL;

    /**
     * Constructor for DownloadAsyncQueryThread.
     * @param queryObj AsyncQuery object
     * @param user Elide User Object
     * @param elide Elide Object
     * @param runner QueryRunner Object for GraphQL executions
     * @param asyncQueryDao AsyncQueryDAO implementation Object
     * @param apiVersion Api Version
     * @param resultStorageEngine ResultStorageEngine implementation Object
     * @param requestURL URL of the AsyncRequest pulled from Request Scope
     */
    public AsyncQueryThread(AsyncQuery queryObj, User user, Elide elide, QueryRunner runner,
            AsyncQueryDAO asyncQueryDao, String apiVersion, ResultStorageEngine resultStorageEngine,
            String requestURL, String downloadURI) {
        super(queryObj, user, elide, runner, asyncQueryDao, apiVersion);
        this.resultStorageEngine = resultStorageEngine;
        this.requestURL = requestURL;
        this.downloadURI = downloadURI != null && !downloadURI.startsWith(FORWARD_SLASH)
                ? FORWARD_SLASH + downloadURI : downloadURI;
    }

    /**
     * This is the main method which processes the Async Query request, executes the query and updates
     * values for AsyncQuery and AsyncQueryResult models accordingly.
     * @return AsyncQueryResult
     * @throws URISyntaxException URISyntaxException
     * @throws IOException IOException
     */
    @Override
    protected AsyncQueryResult processQuery() throws URISyntaxException, IOException {
        boolean isDownload = queryObj.getResultType() == ResultType.DOWNLOAD;

        if (isDownload && resultStorageEngine == null) {
            throw new IllegalStateException("ResultStorageEngine unavailable.");
        }

        AsyncQueryResult queryResultObj = super.processQuery();

        if (isDownload && !isError) {
            queryObj.setResult(queryResultObj);
            // TODO Observable.just will be replaced when we change to make attachment a multi-row entity.
            queryObj = resultStorageEngine.storeResults(queryObj, Observable.just(queryResultObj.getResponseBody()));
            queryResultObj = queryObj.getResult();
            queryResultObj.setResponseBody(generateDownloadUrl(requestURL, queryObj.getId()).toString());
        }

        return queryResultObj;
    }

    /**
     * Download URL generator.
     * @param requestURL original request URL
     * @param asyncQueryID async query ID
     * @return download url
     */
    protected URL generateDownloadUrl(String requestURL, String asyncQueryID) {
        log.debug("generateDownloadUrl");
        String baseURL = requestURL != null ? getBasePath(requestURL) : null;
        String tempURL = baseURL != null && downloadURI != null ? baseURL + downloadURI : null;

        URL url;
        String downloadURL;
        if (tempURL == null) {
            downloadURL = null;
        } else {
            downloadURL = tempURL.endsWith(FORWARD_SLASH) ? tempURL + asyncQueryID
                    : tempURL + FORWARD_SLASH + asyncQueryID;
        }

        try {
            url = new URL(downloadURL);
        } catch (MalformedURLException e) {
            log.error("Exception: {}", e);
            //Results persisted, unable to generate URL
            throw new IllegalStateException(e);
        }
        return url;
    }

    /**
     * This method parses the URL and gets the scheme, host, port.
     * @param URL URL from the Async request
     * @return BasePath extracted from URI
     */
    protected String getBasePath(String URL) {
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
}
