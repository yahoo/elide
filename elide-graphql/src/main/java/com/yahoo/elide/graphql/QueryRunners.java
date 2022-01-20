/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.Elide;
import com.yahoo.elide.RefreshableElide;

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
    public QueryRunners(RefreshableElide refreshableElide) {
        this.runners = new HashMap<>();
        Elide elide = refreshableElide.getElide();

        for (String apiVersion : elide.getElideSettings().getDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion));
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
