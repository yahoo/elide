/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.datastores.aggregation.QueryEngine;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;

/**
 * A {@link QueryResult} contains the results from {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)}.
 * @param <T> The type/model of data being returned.
 */
@Value
@Builder
public class QueryResult<T> implements Serializable {
    private static final long serialVersionUID = -3748307200186480683L;

    @NonNull
    private Iterable<T> data;

    /**
     * Total record count. Null unless Query had Pagination with {@link Pagination#returnPageTotals()} set.
     */
    private Long pageTotals;
}
