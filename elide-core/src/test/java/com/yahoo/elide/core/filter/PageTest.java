/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.pagination.Pagination;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests parsing the page params for json-api pagination.
 */
public class PageTest {

    @Test
    public void shouldParseQueryParamsForCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[number]", "1");

        Pagination pageData = Pagination.parseQueryParams(queryParams);

        Assert.assertEquals(pageData.getPage(), 1);
        Assert.assertEquals(pageData.getPageSize(), 10);
    }

    @Test
    public void shouldParseQueryParamsForOffsetAndLimit() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[limit]", "10");
        queryParams.add("page[offset]", "2");

        Pagination pageData = Pagination.parseQueryParams(queryParams);

        Assert.assertEquals(pageData.getPage(), 2);
        Assert.assertEquals(pageData.getPageSize(), 10);
    }

    @Test
    public void shouldUseDefaultsWhenMissingCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");

        Pagination pageData = Pagination.parseQueryParams(queryParams);

        Assert.assertEquals(pageData.getPage(), 1);
        Assert.assertEquals(pageData.getPageSize(), 10);
    }
}
