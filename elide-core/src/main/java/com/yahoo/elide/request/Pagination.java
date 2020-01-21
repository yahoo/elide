/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.request;

/**
 * Represents a client request to paginate a collection.
 */
public interface Pagination {
    /**
     * Get the page offset.
     * @return record offset.
     */
    Integer getOffset();

    /**
     * Get the page limit.
     * @return record limit.
     */
    Integer getLimit();

    /**
     * Whether or not to fetch the collection size or not.
     * @return true if the client wants the total size of the collection.
     */
    Boolean returnPageTotals();

    /**
     * Get the total size of the collection
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
    public Boolean isDefaultInstance();
}
