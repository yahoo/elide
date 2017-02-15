/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.Elide;
import com.yahoo.elide.annotation.Paginate;
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
    private final Elide.ElideSettings elideSettings =
            new Elide.ElideSettings(Pagination.MAX_PAGE_LIMIT, Pagination.DEFAULT_PAGE_LIMIT);

    @Test
    public void shouldParseQueryParamsForCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[number]", "2");

        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData = pageData.evaluate(PaginationLogicTest.class);
        // page based strategy uses human readable paging - non-zero index
        // page 2 becomes (1)*10 so 10 since we shift to zero based index
        Assert.assertEquals(pageData.getOffset(), 10);
        Assert.assertEquals(pageData.getLimit(), 10);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void shouldThrowExceptionForNegativePageNumber() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[number]", "-2");

        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData.evaluate(PaginationLogicTest.class);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void shouldThrowExceptionForNegativePageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "-10");
        queryParams.add("page[number]", "2");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData.evaluate(PaginationLogicTest.class);
    }

    @Test
    public void shouldParseQueryParamsForOffsetAndLimit() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[limit]", "10");
        queryParams.add("page[offset]", "2");

        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData = pageData.evaluate(PaginationLogicTest.class);
        // offset is direct correlation to start field in query
        Assert.assertEquals(pageData.getOffset(), 2);
        Assert.assertEquals(pageData.getLimit(), 10);
    }

    @Test
    public void shouldUseDefaultsWhenMissingCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        Assert.assertEquals(pageData.getOffset(), Pagination.DEFAULT_OFFSET);
        Assert.assertEquals(pageData.getLimit(), Pagination.DEFAULT_PAGE_LIMIT);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void neverExceedMaxPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "25000");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData.evaluate(PaginationLogicTest.class);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void invalidUsageOfPaginationParameters() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[offset]", "100");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData.evaluate(PaginationLogicTest.class);
    }

    @Test
    public void pageBasedPaginationWithDefaultSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[number]", "2");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData = pageData.evaluate(PaginationLogicTest.class);
        Assert.assertEquals(pageData.getLimit(), Pagination.DEFAULT_PAGE_LIMIT);
        Assert.assertEquals(pageData.getOffset(), Pagination.DEFAULT_PAGE_LIMIT);
    }

    @Test (expectedExceptions = InvalidValueException.class)
    public void shouldThrowExceptionForNonIntPageParamValues() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "2.5");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
    }

    @Test (expectedExceptions = InvalidValueException.class)
    public void shouldThrowExceptionForInvalidPageParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[random]", "1");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
    }

    @Test
    public void shouldSetGenerateTotals() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[totals]", null);
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData = pageData.evaluate(PaginationLogicTest.class);
        Assert.assertTrue(pageData.isGenerateTotals());
    }

    @Test
    public void shouldNotSetGenerateTotals() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        Assert.assertFalse(pageData.isGenerateTotals());
    }


    @Test
    public void shouldUseDefaultsWhenNoParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();

        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        Assert.assertEquals(pageData.getOffset(), 0);
        Assert.assertEquals(pageData.getLimit(), Pagination.DEFAULT_PAGE_LIMIT);

        pageData = Pagination.parseQueryParams(queryParams, new Elide.ElideSettings(10, 10));
        Assert.assertEquals(pageData.getOffset(), 0);
        Assert.assertEquals(pageData.getLimit(), 10);
    }

    @Test
    public void testClassLevelOverride() {
        @Paginate(maxLimit = 100000, defaultLimit = 10)
        class PaginationOverrideTest { }

        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        Pagination pageData = Pagination.parseQueryParams(queryParams, new Elide.ElideSettings(0, 0));
        Assert.assertEquals(pageData.getOffset(), 0);
        Assert.assertEquals(pageData.getLimit(), 0);

        Pagination result = pageData.evaluate(PaginationOverrideTest.class);
        Assert.assertEquals(pageData.getOffset(), 0);
        Assert.assertEquals(result.getLimit(), 10);
    }
}
