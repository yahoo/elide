/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.pagination;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.annotation.Paginate;
import com.yahoo.elide.core.exceptions.InvalidValueException;

import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Encapsulates the pagination strategy.
 */

@ToString
public class Pagination {
    /**
     * Denotes the internal field names for paging.
     */
    public enum PaginationKey { offset, number, size, limit, totals }

    public static final int DEFAULT_OFFSET = 0;
    public static final int DEFAULT_PAGE_LIMIT = 500;
    public static final int MAX_PAGE_LIMIT = 10000;

    // For specifying which page of records is to be returned in the response
    public static final String PAGE_NUMBER_KEY = "page[number]";

    // For specifying the page size - essentially an alias for page[limit]
    public static final String PAGE_SIZE_KEY = "page[size]";

    // For specifying the first row to be returned in the response
    public static final String PAGE_OFFSET_KEY = "page[offset]";

    // For limiting the number of records returned
    public static final String PAGE_LIMIT_KEY = "page[limit]";

    // For requesting total pages/records be included in the response page meta data
    public static final String PAGE_TOTALS_KEY = "page[totals]";

    public static final Map<String, PaginationKey> PAGE_KEYS = new HashMap<>();
    static {
        PAGE_KEYS.put(PAGE_NUMBER_KEY, PaginationKey.number);
        PAGE_KEYS.put(PAGE_SIZE_KEY, PaginationKey.size);
        PAGE_KEYS.put(PAGE_OFFSET_KEY, PaginationKey.offset);
        PAGE_KEYS.put(PAGE_LIMIT_KEY, PaginationKey.limit);
        PAGE_KEYS.put(PAGE_TOTALS_KEY, PaginationKey.totals);
    }

    private long pageTotals = 0;

    private static final String PAGE_KEYS_CSV = PAGE_KEYS.keySet().stream().collect(Collectors.joining(", "));

    // For holding the page query parameters until they can be evaluated
    private Map<PaginationKey, Integer> pageData;

    @Getter
    private int offset;

    @Getter
    private int limit;

    @Getter
    private boolean generateTotals;

    private final int defaultMaxPageSize;
    private final int defaultPageSize;

    private Pagination(Map<PaginationKey, Integer> pageData, int defaultMaxPageSize, int defaultPageSize) {
        this.pageData = pageData;
        this.defaultMaxPageSize = defaultMaxPageSize;
        this.defaultPageSize = defaultPageSize;
    }

    /**
     * Given json-api paging params, generate page and pageSize values from query params.
     *
     * @param queryParams The page queryParams (ImmuatableMultiValueMap).
     * @param elideSettings Elide settings containing pagination default limits
     * @return The new Page object.
     */
    public static Pagination parseQueryParams(final MultivaluedMap<String, String> queryParams,
                                              ElideSettings elideSettings)
            throws InvalidValueException {
        final Map<PaginationKey, Integer> pageData = new HashMap<>();
        queryParams.entrySet()
                .forEach(paramEntry -> {
                    final String queryParamKey = paramEntry.getKey();
                    if (PAGE_KEYS.containsKey(queryParamKey)) {
                        PaginationKey paginationKey = PAGE_KEYS.get(queryParamKey);
                        if (paginationKey.equals(PaginationKey.totals)) {
                            // page[totals] is a valueless parameter, use value of 0 just so that its presence can
                            // be recorded in the map
                            pageData.put(paginationKey, 0);
                        } else {
                            final String value = paramEntry.getValue().get(0);
                            try {
                                int intValue = Integer.parseInt(value, 10);
                                pageData.put(paginationKey, intValue);
                            } catch (NumberFormatException e) {
                                throw new InvalidValueException("page values must be integers");
                            }
                        }
                    } else if (queryParamKey.startsWith("page[")) {
                        throw new InvalidValueException("Invalid Pagination Parameter. Accepted values are "
                                + PAGE_KEYS_CSV);
                    }
                });
        // Decidedly default settings until evaluate is called (a call to evaluate from the datastore will update this):
        Pagination result = new Pagination(pageData,
                elideSettings.getDefaultMaxPageSize(), elideSettings.getDefaultPageSize());
        result.offset = 0;
        result.limit = elideSettings.getDefaultPageSize();
        return result;
    }

    /**
     * Sets the total number of records for the paginated query.
     * @param total
     */
    public void setPageTotals(long total) {
        this.pageTotals = total;
    }

    /**
     * Fetches the total number of records of the paginated query.
     * @return page totals
     */
    public long getPageTotals() {
        return pageTotals;
    }

    /**
     * Evaluates the pagination variables for default limits.
     *
     * @param defaultLimit
     * @param maxLimit
     * @return the calculated {@link Pagination}
     */
    private Pagination evaluate(int defaultLimit, int maxLimit) {

        if (pageData.containsKey(PaginationKey.size) || pageData.containsKey(PaginationKey.number)) {
            // Page-based pagination strategy

            if (pageData.containsKey(PaginationKey.limit) || pageData.containsKey(PaginationKey.offset)) {
                throw new InvalidValueException("Invalid usage of pagination parameters.");
            }

            limit = pageData.containsKey(PaginationKey.size) ? pageData.get(PaginationKey.size) : defaultLimit;
            if (limit > maxLimit) {
                throw new InvalidValueException(
                        "page[size] value must be less than or equal to " + maxLimit);
            } else if (limit < 0) {
                throw new InvalidValueException("page[size] must contain a positive value.");
            }

            int pageNumber = pageData.containsKey(PaginationKey.number) ? pageData.get(PaginationKey.number) : 1;
            if (pageNumber < 1) {
                throw new InvalidValueException("page[number] must contain a positive value.");
            }

            offset = pageNumber > 0 ? (pageNumber - 1) * limit : 0;
        } else if (pageData.containsKey(PaginationKey.limit) || pageData.containsKey(PaginationKey.offset)) {

            // Offset-based pagination strategy
            limit = pageData.containsKey(PaginationKey.limit)
                    ? pageData.get(PaginationKey.limit) : defaultLimit;
            if (limit > maxLimit) {
                throw new InvalidValueException("page[limit] value must be less than or equal to " + maxLimit);
            }

            offset = pageData.containsKey(PaginationKey.offset) ? pageData.get(PaginationKey.offset) : 0;

            if (limit < 0 || offset < 0) {
                throw new InvalidValueException("page[offset] and page[limit] must contain positive values.");
            }

        } else {
            limit = defaultLimit;
            offset = 0;
        }

        generateTotals = pageData.containsKey(PaginationKey.totals);

        return this;
    }

    /**
     * Evaluates the pagination variables. Uses the Paginate annotation if it has been set for the entity to be
     * queried.
     *
     * @param entityClass Entity class to paginate
     * @return the calculated {@link Pagination}
     */
    public Pagination evaluate(final Class entityClass) {
        Paginate paginate =
                entityClass != null ? (Paginate) entityClass.getAnnotation(Paginate.class) : null;

        int defaultLimit = paginate != null ? paginate.defaultLimit() : defaultPageSize;
        int maxLimit = paginate != null ? paginate.maxLimit() : defaultMaxPageSize;

        evaluate(defaultLimit, maxLimit);

        generateTotals = generateTotals && (paginate == null || paginate.countable());

        return this;
    }

    /**
     * Know if this is the default instance.
     * @return The default pagination values.
     */
    public boolean isDefaultInstance() {
        return pageData.isEmpty();
    }

    /**
     * Alias for isDefault.
     * @return true if there are no pagination rules
     */
    public boolean isEmpty() {
        return isDefaultInstance();
    }

    /**
     * Default Instance.
     * @return The default instance.
     */
    public static Pagination getDefaultPagination(ElideSettings elideSettings) {
        Pagination defaultPagination = new Pagination(new HashMap<>(),
                elideSettings.getDefaultMaxPageSize(), elideSettings.getDefaultPageSize());
        defaultPagination.offset = DEFAULT_OFFSET;
        defaultPagination.limit = DEFAULT_PAGE_LIMIT;
        return defaultPagination;
    }
}
