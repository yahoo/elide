/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.QueryEngine;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A {@link QueryResult} contains the results from {@link QueryEngine#executeQuery(Query)}.
 */
@Value
@Builder
public class QueryResult {
    @NonNull
    Iterable<Object> data;
}
