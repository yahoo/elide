/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Paginate;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.pagination.Pagination;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests parsing the page params for json-api pagination.
 */
public class PaginationLogicTest {
    private final ElideSettings elideSettings =
            new ElideSettingsBuilder(null).build();

    @Test
    public void shouldParseQueryParamsForCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[number]", "2");

        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData = pageData.evaluate(PaginationLogicTest.class);
        // page based strategy uses human readable paging - non-zero index
        // page 2 becomes (1)*10 so 10 since we shift to zero based index
        assertEquals(10, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }

    @Test
    public void shouldThrowExceptionForNegativePageNumber() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[number]", "-2");

        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        assertThrows(InvalidValueException.class, () -> pageData.evaluate(PaginationLogicTest.class));
    }

    @Test
    public void shouldThrowExceptionForNegativePageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "-10");
        queryParams.add("page[number]", "2");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        assertThrows(InvalidValueException.class, () -> pageData.evaluate(PaginationLogicTest.class));
    }

    @Test
    public void shouldParseQueryParamsForOffsetAndLimit() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[limit]", "10");
        queryParams.add("page[offset]", "2");

        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData = pageData.evaluate(PaginationLogicTest.class);
        // offset is direct correlation to start field in query
        assertEquals(2, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }

    @Test
    public void shouldUseDefaultsWhenMissingCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        assertEquals(Pagination.DEFAULT_OFFSET, pageData.getOffset());
        assertEquals(Pagination.DEFAULT_PAGE_LIMIT, pageData.getLimit());
    }

    @Test
    public void checkValidOffsetAndFirstRequest() {
        Pagination pageData = Pagination.fromOffsetAndFirst(Optional.of("10"), Optional.of("1"), true, elideSettings).get();

        // NOTE: This is always set to default until evaluate. Then the appropriate value should be used.
        // This is because the particular root entity determines the pagination limits
        assertEquals(0, pageData.getOffset());
        assertEquals(500, pageData.getLimit());

        assertEquals(1, pageData.evaluate(PaginationLogicTest.class).getOffset());
        assertEquals(10, pageData.evaluate(PaginationLogicTest.class).getLimit());
        assertTrue(pageData.evaluate(PaginationLogicTest.class).isGenerateTotals());
    }

    @Test
    public void checkErroneousPageLimit() {
        Pagination pageData =
                Pagination.fromOffsetAndFirst(Optional.of("100000"), Optional.of("1"), false, elideSettings).get();

        // NOTE: This is always set to default until evaluate. Then the appropriate value should be used.
        // This is because the particular root entity determines the pagination limits
        assertEquals(0, pageData.getOffset());
        assertEquals(500, pageData.getLimit());
        assertThrows(
                InvalidValueException.class,
                () -> pageData.evaluate(PaginationLogicTest.class).getOffset());
        assertThrows(
                InvalidValueException.class,
                () -> pageData.evaluate(PaginationLogicTest.class).getLimit());
    }

    @Test
    public void checkBadOffset() {
        assertThrows(
                InvalidValueException.class,
                () -> Pagination.fromOffsetAndFirst(Optional.of("-1"), Optional.of("1000"), false, elideSettings));
    }

    @Test
    public void checkBadOffsetString() {
        assertThrows(
                InvalidValueException.class,
                () -> Pagination.fromOffsetAndFirst(Optional.of("NaN"), Optional.of("1000"), false, elideSettings));
    }

    @Test
    public void checkBadLimit() {
        assertThrows(
                InvalidValueException.class,
                () -> Pagination.fromOffsetAndFirst(Optional.of("0"), Optional.of("1"), false, elideSettings));
    }

    @Test
    public void checkBadLimitString() {
        assertThrows(
                InvalidValueException.class,
                () -> Pagination.fromOffsetAndFirst(Optional.of("1"), Optional.of("NaN"), false, elideSettings));
    }

    @Test
    public void neverExceedMaxPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "25000");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        assertThrows(InvalidValueException.class, () -> pageData.evaluate(PaginationLogicTest.class));
    }

    @Test
    public void invalidUsageOfPaginationParameters() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[offset]", "100");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        assertThrows(InvalidValueException.class, () -> pageData.evaluate(PaginationLogicTest.class));
    }

    @Test
    public void pageBasedPaginationWithDefaultSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[number]", "2");
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData = pageData.evaluate(PaginationLogicTest.class);
        assertEquals(Pagination.DEFAULT_PAGE_LIMIT, pageData.getLimit());
        assertEquals(Pagination.DEFAULT_PAGE_LIMIT, pageData.getOffset());
    }

    @Test
    public void shouldThrowExceptionForNonIntPageParamValues() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "2.5");
        assertThrows(InvalidValueException.class, () -> Pagination.parseQueryParams(queryParams, elideSettings));
    }

    @Test
    public void shouldThrowExceptionForInvalidPageParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[random]", "1");
        assertThrows(InvalidValueException.class, () -> Pagination.parseQueryParams(queryParams, elideSettings));
    }

    @Test
    public void shouldSetGenerateTotals() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[totals]", null);
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        pageData = pageData.evaluate(PaginationLogicTest.class);
        assertTrue(pageData.isGenerateTotals());
    }

    @Test
    public void shouldNotSetGenerateTotals() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        assertFalse(pageData.isGenerateTotals());
    }


    @Test
    public void shouldUseDefaultsWhenNoParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();

        Pagination pageData = Pagination.parseQueryParams(queryParams, elideSettings);
        assertEquals(0, pageData.getOffset());
        assertEquals(Pagination.DEFAULT_PAGE_LIMIT, pageData.getLimit());

        pageData = Pagination.parseQueryParams(queryParams,
                new ElideSettingsBuilder(null)
                    .withDefaultPageSize(10)
                    .withDefaultMaxPageSize(10)
                    .build());
        assertEquals(0, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }

    @Test
    public void testClassLevelOverride() {
        @Paginate(maxLimit = 100000, defaultLimit = 10)
        class PaginationOverrideTest { }

        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        Pagination pageData = Pagination.parseQueryParams(queryParams,
                new ElideSettingsBuilder(null)
                    .withDefaultPageSize(0)
                    .withDefaultMaxPageSize(0)
                    .build());
        assertEquals(0, pageData.getOffset());
        assertEquals(0, pageData.getLimit());

        Pagination result = pageData.evaluate(PaginationOverrideTest.class);
        assertEquals(0, pageData.getOffset());
        assertEquals(10, result.getLimit());
    }
}
