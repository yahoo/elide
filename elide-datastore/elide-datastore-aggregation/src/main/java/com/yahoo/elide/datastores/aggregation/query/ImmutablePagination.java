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
    private String before;
    private String after;
    private Direction direction;

    public ImmutablePagination(int offset, int limit, boolean defaultInstance, boolean returnPageTotals, String before,
            String after,
            Direction direction) {
        super();
        this.offset = offset;
        this.limit = limit;
        this.defaultInstance = defaultInstance;
        this.returnPageTotals = returnPageTotals;
        this.before = before;
        this.after = after;
        this.direction = direction;
    }

    public ImmutablePagination(int offset, int limit, boolean defaultInstance, boolean returnPageTotals) {
        this(offset, limit, defaultInstance, returnPageTotals, null, null, null);
    }

    public static ImmutablePagination from(Pagination src) {
        if (src instanceof ImmutablePagination) {
            return (ImmutablePagination) src;
        }
        if (src != null) {
            return new ImmutablePagination(src.getOffset(), src.getLimit(), src.isDefaultInstance(),
                    src.returnPageTotals(), src.getBefore(), src.getAfter(), src.getDirection());
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

    @Override
    public void setStartCursor(String cursor) {
        throw new UnsupportedOperationException("ImmutablePagination does not support setStartCursor");
    }

    @Override
    public void setEndCursor(String cursor) {
        throw new UnsupportedOperationException("ImmutablePagination does not support setEndCursor");
    }

    @Override
    public void setHasPreviousPage(Boolean hasPreviousPage) {
        throw new UnsupportedOperationException("ImmutablePagination does not support setHasPreviousPage");
    }

    @Override
    public void setHasNextPage(Boolean hasNextPage) {
        throw new UnsupportedOperationException("ImmutablePagination does not support setHasNextPage");
    }

    @Override
    public String getStartCursor() {
        return null;
    }

    @Override
    public String getEndCursor() {
        return null;
    }

    @Override
    public Boolean getHasPreviousPage() {
        return null;
    }

    @Override
    public Boolean getHasNextPage() {
        return null;
    }
}
