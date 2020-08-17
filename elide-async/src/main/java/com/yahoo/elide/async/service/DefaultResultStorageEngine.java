/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.request.EntityProjection;

import org.apache.http.client.utils.URIBuilder;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;

import javax.inject.Singleton;
import javax.sql.rowset.serial.SerialBlob;

/**
 * Default implementation of ResultStorageEngine.
 * It supports Async Module to store results with async query.
 */
@Singleton
@Slf4j
@Getter
public class DefaultResultStorageEngine implements ResultStorageEngine {
    private static final String FORWARD_SLASH = "/";

    @Setter private String downloadURI;
    @Setter private ElideSettings elideSettings;
    @Setter private DataStore dataStore;

    public DefaultResultStorageEngine() {
    }

    /**
     * Constructor.
     * @param downloadURI URI Path for the download controller. For example /api/v1/download
     * @param elideSettings ElideSettings object
     * @param dataStore DataStore Object
     */
    public DefaultResultStorageEngine(String downloadURI, ElideSettings elideSettings, DataStore dataStore) {
        this.downloadURI = downloadURI != null && !downloadURI.startsWith(FORWARD_SLASH)
                ? FORWARD_SLASH + downloadURI : downloadURI;
        this.elideSettings = elideSettings;
        this.dataStore = dataStore;
    }

    @Override
    public AsyncQueryResult storeResults(AsyncQueryResult asyncQueryResult, String result, String asyncQueryId) {
        log.debug("store AsyncResults for Download");

        byte[] temp = result.getBytes();
        Blob attachment;
        try {
            attachment = new SerialBlob(temp);
        } catch (SQLException e) {
            log.error("Exception: {}", e);
            throw new IllegalStateException(e);
        }

        asyncQueryResult.setAttachment(attachment);

        return asyncQueryResult;
    }

    @Override
    public byte[] getResultsByID(String asyncQueryID) {
        log.debug("getAsyncResultsByID");

        AsyncQuery asyncQuery = null;
        byte[] byteResult = null;

        try {
            asyncQuery = (AsyncQuery) DBUtil.executeInTransaction(elideSettings,
                    dataStore, (tx, scope) -> {

                        EntityProjection asyncQueryCollection = EntityProjection.builder()
                                .type(AsyncQuery.class)
                                .build();

                        Object loaded = tx.loadObject(asyncQueryCollection, asyncQueryID, scope);

                        return loaded;
                    });
            if (asyncQuery != null && asyncQuery.getResult().getAttachment() != null) {
                Blob result = asyncQuery.getResult().getAttachment();
                byteResult = result.getBytes(1, (int) result.length());
            }
        } catch (SQLException e) {
            log.error("Exception: {}", e);
            throw new IllegalStateException(e);
        }

        return byteResult;
    }

    @Override
    public void deleteResultsCollection(Collection<AsyncQuery> asyncQueryList) {
        log.debug("deleteAsyncQueryResultsCollection");
    }

    @Override
    public URL generateDownloadUrl(String requestURL, String asyncQueryID) {
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
     * @throws URISyntaxException URISyntaxException from malformed or incorrect URI
     * @return BasePath extracted from URI
     */
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
}
