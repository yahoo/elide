/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.pagination;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the pagination strategy.
 */

@AllArgsConstructor
@ToString
public class Pagination {

    /**
     * Denotes the internal field names for paging.
     */
    public enum PaginationKey { page, pageSize }

    public static final int DEFAULT_PAGE_SIZE = 500;
    public static final int MAX_PAGE_SIZE = 10000;

    private static final Pagination DEFAULT_PAGINATION = new Pagination(0, 0);

    public static final Map<String, PaginationKey> PAGE_KEYS = new HashMap<>();
    static {
        PAGE_KEYS.put("page[size]", PaginationKey.pageSize);
        PAGE_KEYS.put("page[limit]", PaginationKey.pageSize);
        PAGE_KEYS.put("page[number]", PaginationKey.page);
        PAGE_KEYS.put("page[offset]", PaginationKey.page);
    }

    @Getter
    private int page;

    @Getter
    private int pageSize;

    /**
     * Know if this is the default instance.
     * @return The default pagination values.
     */
    public boolean isDefault() {
        return this.page == 0 && this.pageSize == 0;
    }

    /**
     * Alias for isDefault.
     * @return true if there are no pagination rules
     */
    public boolean isEmpty() {
        return isDefault();
    }

    /**
     * Given json-api paging params, generate page and pageSize values from query params.
     * @param queryParams The page queryParams (ImmuatableMultiValueMap).
     * @return The new Page object.
     */
    public static Pagination parseQueryParams(final MultivaluedMap<String, String> queryParams)
            throws InvalidValueException {
        final Map<PaginationKey, Integer> pageData = new HashMap<>();
        queryParams.entrySet()
                .forEach(paramEntry -> {
                    final String queryParamKey = paramEntry.getKey();
                    if (PAGE_KEYS.containsKey(queryParamKey)) {
                        final String value = paramEntry.getValue().get(0);
                        try {
                            pageData.put(PAGE_KEYS.get(queryParamKey), Integer.parseInt(value, 10));
                        } catch (ClassCastException e) {
                            throw new InvalidValueException("page values must be integers");
                        }
                    } else if (queryParamKey.startsWith("page[")) {
                        throw new InvalidValueException("Invalid Pagination Parameter. Accepted values are page[number]"
                                + ",page[size],page[offset],page[limit]");
                    }
                });

        if (pageData.isEmpty()) {
            return DEFAULT_PAGINATION;
        }
        if (!pageData.containsKey(PaginationKey.page)) {
            pageData.put(PaginationKey.page, 1);
        }
        if (!pageData.containsKey(PaginationKey.pageSize)) {
            pageData.put(PaginationKey.pageSize, DEFAULT_PAGE_SIZE);
        }
        return new Pagination(pageData.get(PaginationKey.page), pageData.get(PaginationKey.pageSize));
    }

    /**
     * Default Instance.
     * @return The default instance.
     */
    public static Pagination getDefaultPagination() {
        return DEFAULT_PAGINATION;
    }
}
