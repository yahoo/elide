/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.request;

/**
 * Represents a client request to paginate a collection.
 */
public interface Pagination {

    /**
     * Default offset (in records) it client does not provide one.
     */
    int DEFAULT_OFFSET = 0;

    /**
     * Default page limit (in records) it client does not provide one.
     */
    int DEFAULT_PAGE_SIZE = 500;

    /**
     * Maximum allowable page limit (in records).
     */
    int MAX_PAGE_SIZE = 10000;

    /**
     * Get the page offset.
     * @return record offset.
     */
    int getOffset();

    /**
     * Get the page limit.
     * @return record limit.
     */
    int getLimit();

    /**
     * Whether or not to fetch the collection size or not.
     * @return true if the client wants the total size of the collection.
     */
    boolean returnPageTotals();

    /**
     * Get the total size of the collection.
     * @return total record count.
     */
    Long getPageTotals();

    /**
     * Set the total size of the collection.
     * @param pageTotals the total size.
     */
    void setPageTotals(Long pageTotals);

    /**
     * Is this the default instance (not present).
     * @return true if pagination wasn't requested.  False otherwise.
     */
    boolean isDefaultInstance();

    /**
     * The direction for cursor pagination.
     */
    enum Direction {
        FORWARD,
        BACKWARD,
        BETWEEN
    }

    /**
     * Gets the cursor for cursor pagination.
     *
     * @return the cursor
     */
    default String getCursor() {
        String before = getBefore();
        String after = getAfter();
        if (before != null && after != null) {
            throw new IllegalArgumentException("Both before and after cursors exist.");
        }
        if (before != null) {
            return before;
        }
        return after;
    }

    /**
     * Gets the before cursor for cursor pagination.
     *
     * @return the before cursor
     */
    String getBefore();

    /**
     * Gets the after cursor for cursor pagination.
     *
     * @return the after cursor
     */
    String getAfter();

    /**
     * Gets the direction for cursor pagination.
     *
     * @return the direction for cursor pagination.
     */
    Direction getDirection();

    /**
     * Sets the cursor for the first item for cursor pagination.
     */
    void setStartCursor(String cursor);

    /**
     * Sets the cursor for the last item for cursor pagination.
     */
    void setEndCursor(String cursor);

    /**
     * Gets the cursor for the first item for cursor pagination.
     */
    String getStartCursor();

    /**
     * Gets the cursor for the last item for cursor pagination.
     */
    String getEndCursor();

    /**
     * Sets whether there is a previous page for cursor pagination.
     */
    void setHasPreviousPage(Boolean hasPreviousPage);

    /**
     * Gets whether there is a previous page for cursor pagination.
     */
    Boolean getHasPreviousPage();

    /**
     * Sets whether there is a next page for cursor pagination.
     */
    void setHasNextPage(Boolean hasNextPage);

    /**
     * Gets whether there is a next page for cursor pagination.
     */
    Boolean getHasNextPage();
}
