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
import com.yahoo.elide.core.pagination.PaginationImpl;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests parsing the page params for json-api pagination.
 */
public class PaginationImplTest {
    private final ElideSettings elideSettings =
            new ElideSettingsBuilder(null).build();

    @Test
    public void shouldParseQueryParamsForCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[number]", "2");

        PaginationImpl pageData = PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings);
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

        assertThrows(InvalidValueException.class, () -> PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings));
    }

    @Test
    public void shouldThrowExceptionForNegativePageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "-10");
        queryParams.add("page[number]", "2");

        assertThrows(InvalidValueException.class, () -> PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings));
    }

    @Test
    public void shouldParseQueryParamsForOffsetAndLimit() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[limit]", "10");
        queryParams.add("page[offset]", "2");

        PaginationImpl pageData = PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings);
        // offset is direct correlation to start field in query
        assertEquals(2, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }

    @Test
    public void shouldUseDefaultsWhenMissingCurrentPageAndPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        PaginationImpl pageData = PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings);
        assertEquals(PaginationImpl.DEFAULT_OFFSET, pageData.getOffset());
        assertEquals(PaginationImpl.DEFAULT_PAGE_LIMIT, pageData.getLimit());
    }

    @Test
    public void checkValidOffsetAndFirstRequest() {
        PaginationImpl pageData = new PaginationImpl(PaginationImplTest.class,
                1,
                10,
                elideSettings.getDefaultPageSize(),
                elideSettings.getDefaultMaxPageSize(),
                false,
                false);

        assertEquals(1, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }

    @Test
    public void checkErroneousPageLimit() {
        assertThrows(
                InvalidValueException.class,
                () -> new PaginationImpl(PaginationImplTest.class,
                10,
                100000,
                elideSettings.getDefaultPageSize(),
                elideSettings.getDefaultMaxPageSize(),
                false,
                false));
    }

    @Test
    public void checkBadOffset() {
        assertThrows(
                InvalidValueException.class,
                () -> new PaginationImpl(PaginationImplTest.class,
                        -1,
                        1000,
                        elideSettings.getDefaultPageSize(),
                        elideSettings.getDefaultMaxPageSize(),
                        false,
                        false));
    }

    @Test
    public void checkBadLimit() {
        assertThrows(
                InvalidValueException.class,
                () -> new PaginationImpl(PaginationImplTest.class,
                        0,
                        -1,
                        elideSettings.getDefaultPageSize(),
                        elideSettings.getDefaultMaxPageSize(),
                        false,
                        false));
    }

    @Test
    public void neverExceedMaxPageSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "25000");
        assertThrows(InvalidValueException.class,
                () -> PaginationImpl.parseQueryParams(PaginationImplTest.class,
                        Optional.of(queryParams), elideSettings));
    }

    @Test
    public void invalidUsageOfPaginationParameters() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "10");
        queryParams.add("page[offset]", "100");
        assertThrows(InvalidValueException.class,
                () -> PaginationImpl.parseQueryParams(PaginationImplTest.class,
                        Optional.of(queryParams), elideSettings));
    }

    @Test
    public void pageBasedPaginationWithDefaultSize() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[number]", "2");
        PaginationImpl pageData = PaginationImpl.parseQueryParams(PaginationImpl.class,
                Optional.of(queryParams), elideSettings);
        assertEquals(PaginationImpl.DEFAULT_PAGE_LIMIT, pageData.getLimit());
        assertEquals(PaginationImpl.DEFAULT_PAGE_LIMIT, pageData.getOffset());
    }

    @Test
    public void shouldThrowExceptionForNonIntPageParamValues() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[size]", "2.5");
        assertThrows(InvalidValueException.class,
                () -> PaginationImpl.parseQueryParams(PaginationImplTest.class,
                        Optional.of(queryParams), elideSettings));
    }

    @Test
    public void shouldThrowExceptionForInvalidPageParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[random]", "1");
        assertThrows(InvalidValueException.class,
        () -> PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings));
    }

    @Test
    public void shouldSetGenerateTotals() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        queryParams.add("page[totals]", null);
        PaginationImpl pageData = PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings);
        assertTrue(pageData.returnPageTotals());
    }

    @Test
    public void shouldNotSetGenerateTotals() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();
        PaginationImpl pageData = PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings);
        assertFalse(pageData.returnPageTotals());
    }


    @Test
    public void shouldUseDefaultsWhenNoParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedStringMap();

        PaginationImpl pageData = PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), elideSettings);
        assertEquals(0, pageData.getOffset());
        assertEquals(PaginationImpl.DEFAULT_PAGE_LIMIT, pageData.getLimit());

        pageData = PaginationImpl.parseQueryParams(PaginationImplTest.class,
                Optional.of(queryParams), new ElideSettingsBuilder(null)
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
        PaginationImpl pageData = PaginationImpl.parseQueryParams(PaginationOverrideTest.class,
                Optional.of(queryParams),
                new ElideSettingsBuilder(null)
                    .withDefaultPageSize(1)
                    .withDefaultMaxPageSize(1)
                    .build());

        assertEquals(0, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }
}
