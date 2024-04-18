/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql;

import com.paiondata.elide.Elide;
import com.paiondata.elide.RefreshableElide;

import graphql.execution.DataFetcherExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps API version to a GraphQL query runner.  This class is hot reloadable and must be restricted to a single
 * access method.
 */
public class QueryRunners {
    private final Map<String, QueryRunner> runners;

    /**
     * Constructor.
     * @param refreshableElide A hot reloadable Elide instance.
     */
    public QueryRunners(RefreshableElide refreshableElide, DataFetcherExceptionHandler exceptionHandler) {
        this.runners = new HashMap<>();
        Elide elide = refreshableElide.getElide();

        for (String apiVersion : elide.getElideSettings().getEntityDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion, exceptionHandler));
        }
    }

    /**
     * Gets a runner for a given API version.  This is the ONLY access method for this class to
     * eliminate state issues across reloads.
     * @param apiVersion The api version.
     * @return The associated query runner.
     */
    public QueryRunner getRunner(String apiVersion) {
        return runners.get(apiVersion);
    }
}
