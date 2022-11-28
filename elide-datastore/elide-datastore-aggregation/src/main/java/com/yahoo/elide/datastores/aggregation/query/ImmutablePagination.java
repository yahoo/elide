/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.request.Pagination;

import lombok.Value;

/**
 * An immutable Pagination. Doesn't support getPageTotals/setPageTotals; page totals must be returned via QueryResult.
 */
@Value
public class ImmutablePagination implements Pagination {

    private int offset;
    private int limit;
    private boolean defaultInstance;
    private boolean returnPageTotals;

    public static ImmutablePagination from(Pagination src) {
        if (src instanceof ImmutablePagination) {
            return (ImmutablePagination) src;
        }
        if (src != null) {
            return new ImmutablePagination(
                    src.getOffset(), src.getLimit(), src.isDefaultInstance(), src.returnPageTotals());
        }
        return null;
    }

    @Override
    public boolean returnPageTotals() {
        return returnPageTotals;
    }

    @Override
    public Long getPageTotals() {
        return null;
    }

    @Override
    public void setPageTotals(Long pageTotals) {
        throw new UnsupportedOperationException("ImmutablePagination does not support setPageTotals");
    }
}
