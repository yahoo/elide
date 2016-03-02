/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.pagination.Pagination;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests parsing the page params for json-api pagination.
 */
public class PaginationLogicTest {

    @Test
    public void shouldParseQueryParamsForCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[number]", "2");

        Pagination pageData = Pagination.parseQueryParams(queryParams);
        // page based strategy uses human readable paging - non-zero index
        // page 2 becomes (1)*10 so 10 since we shift to zero based index
        Assert.assertEquals(pageData.getOffset(), 10);
        Assert.assertEquals(pageData.getLimit(), 10);
    }

    @Test
    public void shouldIgnoreNegativeIndexForCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[number]", "-2");

        Pagination pageData = Pagination.parseQueryParams(queryParams);
        // page based strategy uses human readable paging - non-zero index
        // page 2 becomes (1)*10 so 10 since we shift to zero based index
        Assert.assertEquals(pageData.getOffset(), 0);
        Assert.assertEquals(pageData.getLimit(), 10);
    }

    @Test
    public void shouldThrowExceptionForNegativePageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "-10");
        queryParams.add("page[number]", "2");

        try {
            Pagination pageData = Pagination.parseQueryParams(queryParams);
            Assert.assertTrue(false);
        } catch (InvalidValueException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldParseQueryParamsForOffsetAndLimit() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[limit]", "10");
        queryParams.add("page[offset]", "2");

        Pagination pageData = Pagination.parseQueryParams(queryParams);
        // offset is direct correlation to start field in query
        Assert.assertEquals(pageData.getOffset(), 2);
        Assert.assertEquals(pageData.getLimit(), 10);
    }

    @Test
    public void shouldUseDefaultsWhenMissingCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");

        Pagination pageData = Pagination.parseQueryParams(queryParams);
        // size = limit, so start 0, limit 10
        Assert.assertEquals(pageData.getOffset(), 0);
        Assert.assertEquals(pageData.getLimit(), 10);
    }
}
