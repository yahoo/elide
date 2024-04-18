/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.request;

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
}
