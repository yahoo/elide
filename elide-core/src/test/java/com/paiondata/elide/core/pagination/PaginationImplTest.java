/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.annotation.Paginate;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.type.ClassType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests parsing the page params for json-api pagination.
 */
public class PaginationImplTest {
    private final ElideSettings elideSettings =
            ElideSettings.builder().dataStore(null)
                    .entityDictionary(EntityDictionary.builder().build())
                    .build();
    static void add(Map<String, List<String>> params, String key, String value) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    @Test
    public void shouldParseQueryParamsForCurrentPageAndPageSize() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[size]", "10");
        add(queryParams, "page[number]", "2");

        PaginationImpl pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings);
        // page based strategy uses human readable paging - non-zero index
        // page 2 becomes (1)*10 so 10 since we shift to zero based index
        assertEquals(10, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }

    @Test
    public void shouldThrowExceptionForNegativePageNumber() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[size]", "10");
        add(queryParams, "page[number]", "-2");

        assertThrows(InvalidValueException.class, () -> PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings));
    }

    @Test
    public void shouldThrowExceptionForNegativePageSize() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[size]", "-10");
        add(queryParams, "page[number]", "2");

        assertThrows(InvalidValueException.class, () -> PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings));
    }

    @Test
    public void shouldParseQueryParamsForOffsetAndLimit() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[limit]", "10");
        add(queryParams, "page[offset]", "2");

        PaginationImpl pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings);
        // offset is direct correlation to start field in query
        assertEquals(2, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }

    @Test
    public void shouldUseDefaultsWhenMissingCurrentPageAndPageSize() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        PaginationImpl pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings);
        assertEquals(PaginationImpl.DEFAULT_OFFSET, pageData.getOffset());
        assertEquals(PaginationImpl.DEFAULT_PAGE_SIZE, pageData.getLimit());
    }

    @Test
    public void checkValidOffsetAndFirstRequest() {
        PaginationImpl pageData = new PaginationImpl(PaginationImplTest.class,
                1,
                10,
                elideSettings.getDefaultPageSize(),
                elideSettings.getMaxPageSize(),
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
                elideSettings.getMaxPageSize(),
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
                        elideSettings.getMaxPageSize(),
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
                        elideSettings.getMaxPageSize(),
                        false,
                        false));
    }

    @Test
    public void neverExceedMaxPageSize() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[size]", "25000");
        assertThrows(InvalidValueException.class,
                () -> PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                        queryParams, elideSettings));
    }

    @Test
    public void invalidUsageOfPaginationParameters() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[size]", "10");
        add(queryParams, "page[offset]", "100");
        assertThrows(InvalidValueException.class,
                () -> PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                        queryParams, elideSettings));
    }

    @Test
    public void pageBasedPaginationWithDefaultSize() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[number]", "2");
        PaginationImpl pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationImpl.class),
                queryParams, elideSettings);
        assertEquals(PaginationImpl.DEFAULT_PAGE_SIZE, pageData.getLimit());
        assertEquals(PaginationImpl.DEFAULT_PAGE_SIZE, pageData.getOffset());
    }

    @Test
    public void shouldThrowExceptionForNonIntPageParamValues() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[size]", "2.5");
        assertThrows(InvalidValueException.class,
                () -> PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                        queryParams, elideSettings));
    }

    @Test
    public void shouldThrowExceptionForInvalidPageParams() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[random]", "1");
        assertThrows(InvalidValueException.class,
        () -> PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings));
    }

    @Test
    public void shouldSetGenerateTotals() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "page[totals]", null);
        PaginationImpl pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings);
        assertTrue(pageData.returnPageTotals());
    }

    @Test
    public void shouldNotSetGenerateTotals() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        PaginationImpl pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings);
        assertFalse(pageData.returnPageTotals());
    }


    @Test
    public void shouldUseDefaultsWhenNoParams() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        PaginationImpl pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, elideSettings);
        assertEquals(0, pageData.getOffset());
        assertEquals(PaginationImpl.DEFAULT_PAGE_SIZE, pageData.getLimit());

        pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationImplTest.class),
                queryParams, ElideSettings.builder().dataStore(null)
                    .entityDictionary(EntityDictionary.builder().build())
                    .defaultPageSize(10)
                    .maxPageSize(10)
                    .build());
        assertEquals(0, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }

    @Test
    public void testClassLevelOverride() {
        @Paginate(maxPageSize = 100000, defaultPageSize = 10)
        class PaginationOverrideTest { }

        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        PaginationImpl pageData = PaginationImpl.parseQueryParams(ClassType.of(PaginationOverrideTest.class),
                queryParams,
                ElideSettings.builder().dataStore(null)
                    .entityDictionary(EntityDictionary.builder().build())
                    .defaultPageSize(1)
                    .maxPageSize(1)
                    .build());

        assertEquals(0, pageData.getOffset());
        assertEquals(10, pageData.getLimit());
    }
}
