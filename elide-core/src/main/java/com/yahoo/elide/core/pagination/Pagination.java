/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.pagination;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import lombok.Getter;
import lombok.ToString;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the pagination strategy.
 */

@ToString
public class Pagination {

    @Getter
    private int offset;

    @Getter
    private int limit;

    public Pagination(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * Denotes the internal field names for paging.
     */
    public enum PaginationKey { offset, number, size, limit }

    public static final int DEFAULT_OFFSET = 0;
    public static final int DEFAULT_PAGE_LIMIT = 500;
    public static final int MAX_PAGE_LIMIT = 10000;

    private static final Pagination DEFAULT_PAGINATION = new Pagination(DEFAULT_OFFSET, DEFAULT_PAGE_LIMIT);

    public static final Map<String, PaginationKey> PAGE_KEYS = new HashMap<>();
    static {
        PAGE_KEYS.put("page[number]", PaginationKey.number);
        PAGE_KEYS.put("page[size]", PaginationKey.size);
        PAGE_KEYS.put("page[offset]", PaginationKey.offset);
        PAGE_KEYS.put("page[limit]", PaginationKey.limit);
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
                        } catch (NumberFormatException e) {
                            throw new InvalidValueException("page values must be integers");
                        }
                    } else if (queryParamKey.startsWith("page[")) {
                        throw new InvalidValueException("Invalid Pagination Parameter. Accepted values are page[number]"
                                + ",page[size],page[offset],page[limit]");
                    }
                });

        if (pageData.isEmpty()) {
            return DEFAULT_PAGINATION;
        } else if (pageData.size() > 2) {
            throw new InvalidValueException("Invalid usage of pagination parameters.");
        }

        if (isPageBased(pageData)) {

            // Page-based pagination strategy
            int limit = pageData.get(PaginationKey.size);
            if (limit > MAX_PAGE_LIMIT) {
                throw new InvalidValueException("page[size] value must be less than or equal to " + MAX_PAGE_LIMIT);
            } else if (limit < 0) {
                throw new InvalidValueException("page[size] must contain a positive value.");
            }

            int pageNumber = pageData.containsKey(PaginationKey.number) ? pageData.get(PaginationKey.number) : 1;
            if (pageNumber < 1) {
                throw new InvalidValueException("page[number] must contain a positive value.");
            }

            int computedOffset = pageNumber > 0 ? (pageNumber - 1) * limit : 0;
            return new Pagination(computedOffset, limit);

        } else if (isOffsetBased(pageData)) {

            // Offset-based pagination strategy
            int limit = pageData.containsKey(PaginationKey.limit)
                    ? pageData.get(PaginationKey.limit) : DEFAULT_PAGE_LIMIT;

            int offset = pageData.containsKey(PaginationKey.offset) ? pageData.get(PaginationKey.offset) : 0;

            if (limit < 0 || offset < 0) {
                throw new InvalidValueException("page[offset] and page[limit] must contain positive values.");
            }
            return new Pagination(offset, limit);

        }

        // Default Pagination
        return DEFAULT_PAGINATION;
    }

    private static boolean isPageBased(Map<PaginationKey, Integer> pageData) {
        if (pageData.containsKey(PaginationKey.size)) {
            if (pageData.size() == 1 || pageData.containsKey(PaginationKey.number)) {
                // Either Only contains page[size] in params
                // Or contains Both page[size] and page[number]
                return true;
            } else {
                throw new InvalidValueException("Invalid usage of pagination parameters.");
            }
        } else if (pageData.containsKey(PaginationKey.number)) {
            // Contains page[number] but not page[size]
            throw new InvalidValueException("page[size] parameter is required for page-based pagination.");
        }
        return false;
    }

    private static boolean isOffsetBased(Map<PaginationKey, Integer> pageData) {
        return  (pageData.containsKey(PaginationKey.limit) || pageData.containsKey(PaginationKey.offset));
    }

    /**
     * Default Instance.
     * @return The default instance.
     */
    public static Pagination getDefaultPagination() {
        return DEFAULT_PAGINATION;
    }
}
