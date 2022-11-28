/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.pagination;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.annotation.Paginate;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.google.common.collect.ImmutableMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Holds state associated with pagination.
 */
@ToString
@EqualsAndHashCode
public class PaginationImpl implements Pagination {
    /**
     * Denotes the internal field names for paging.
     */
    public enum PaginationKey { offset, number, size, limit, totals }

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

    public static final Map<String, PaginationKey> PAGE_KEYS = ImmutableMap.of(
            PAGE_NUMBER_KEY, PaginationKey.number,
            PAGE_SIZE_KEY, PaginationKey.size,
            PAGE_OFFSET_KEY, PaginationKey.offset,
            PAGE_LIMIT_KEY, PaginationKey.limit,
            PAGE_TOTALS_KEY, PaginationKey.totals);

    @Getter
    @Setter
    private Long pageTotals = 0L;

    private static final String PAGE_KEYS_CSV = PAGE_KEYS.keySet().stream().collect(Collectors.joining(", "));

    @Getter
    private final int offset;

    @Getter
    private final int limit;

    private final boolean generateTotals;

    @Getter
    private final boolean defaultInstance;

    @Getter
    private final Type<?> entityClass;

    /**
     * Constructor.
     * @param entityClass The type of collection we are paginating.
     * @param clientOffset The client requested offset or null if not provided.
     * @param clientLimit The client requested limit or null if not provided.
     * @param systemDefaultLimit The system default limit (in terms of records).
     * @param systemMaxLimit The system max limit (in terms of records).
     * @param generateTotals Whether to return the total number of records.
     * @param pageByPages Whether to page by pages or records.
     */
    public PaginationImpl(Class<?> entityClass,
                          Integer clientOffset,
                          Integer clientLimit,
                          int systemDefaultLimit,
                          int systemMaxLimit,
                          Boolean generateTotals,
                          Boolean pageByPages) {
        this(ClassType.of(entityClass), clientOffset, clientLimit,
                systemDefaultLimit, systemMaxLimit, generateTotals, pageByPages);
    }

    /**
     * Constructor.
     * @param entityClass The type of collection we are paginating.
     * @param clientOffset The client requested offset or null if not provided.
     * @param clientLimit The client requested limit or null if not provided.
     * @param systemDefaultLimit The system default limit (in terms of records).
     * @param systemMaxLimit The system max limit (in terms of records).
     * @param generateTotals Whether to return the total number of records.
     * @param pageByPages Whether to page by pages or records.
     */
    public PaginationImpl(Type<?> entityClass,
                           Integer clientOffset,
                           Integer clientLimit,
                           int systemDefaultLimit,
                           int systemMaxLimit,
                           Boolean generateTotals,
                           Boolean pageByPages) {

        this.entityClass = entityClass;
        this.defaultInstance = (clientOffset == null && clientLimit == null && generateTotals == null);

        Paginate paginate = entityClass != null ? (Paginate) entityClass.getAnnotation(Paginate.class) : null;

        this.limit = clientLimit != null
                ? clientLimit
                : (paginate != null ? paginate.defaultLimit() : systemDefaultLimit);

        int maxLimit = paginate != null ? paginate.maxLimit() : systemMaxLimit;

        String pageSizeLabel = pageByPages ? "size" : "limit";

        if (limit > maxLimit && !defaultInstance) {
            throw new InvalidValueException("Pagination "
                    + pageSizeLabel + " must be less than or equal to " + maxLimit);
        }
        if (limit < 1) {
            throw new InvalidValueException("Pagination "
                    + pageSizeLabel + " must contain a positive, non-zero value.");
        }

        this.generateTotals = generateTotals != null && generateTotals && (paginate == null || paginate.countable());

        if (pageByPages) {
            int pageNumber = clientOffset != null ? clientOffset : 1;
            if (pageNumber < 1) {
                throw new InvalidValueException("Pagination number must be a positive, non-zero value.");
            }
            this.offset = (pageNumber - 1) * limit;
        } else {
            this.offset = clientOffset != null ? clientOffset : 0;

            if (offset < 0) {
                throw new InvalidValueException("Pagination offset must contain a positive value.");
            }
        }
    }

    /**
     * Whether or not the client requested to return page totals.
     * @return true if page totals should be returned.
     */
    @Override
    public boolean returnPageTotals() {
        return generateTotals;
    }

    /**
     * Given json-api paging params, generate page and pageSize values from query params.
     *
     * @param entityClass The collection type.
     * @param queryParams The page queryParams.
     * @param elideSettings Elide settings containing pagination default limits
     * @return The new Pagination object.
     * @throws InvalidValueException invalid query parameter
     */
    public static PaginationImpl parseQueryParams(Type<?> entityClass,
                                                  final MultivaluedMap<String, String> queryParams,
                                                  ElideSettings elideSettings)
            throws InvalidValueException {

        if (queryParams.isEmpty()) {
            return getDefaultPagination(entityClass, elideSettings);
        }

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
        return getPagination(entityClass, pageData, elideSettings);
    }


    /**
     * Construct a pagination object from page data and elide settings.
     *
     * @param entityClass The collection type.
     * @param pageData Map containing pagination information
     * @param elideSettings Settings containing pagination defaults
     * @return Pagination object
     */
    private static PaginationImpl getPagination(Type<?> entityClass, Map<PaginationKey, Integer> pageData,
                                                ElideSettings elideSettings) {
        if (hasInvalidCombination(pageData)) {
            throw new InvalidValueException("Invalid usage of pagination parameters.");
        }

        boolean pageByPages = false;
        Integer offset = pageData.getOrDefault(PaginationKey.offset, null);
        Integer limit = pageData.getOrDefault(PaginationKey.limit, null);

        if (pageData.containsKey(PaginationKey.size) || pageData.containsKey(PaginationKey.number)) {
            pageByPages = true;
            offset = pageData.getOrDefault(PaginationKey.number, null);
            limit = pageData.getOrDefault(PaginationKey.size, null);
        }

        return new PaginationImpl(entityClass,
                offset,
                limit,
                elideSettings.getDefaultPageSize(),
                elideSettings.getDefaultMaxPageSize(),
                pageData.containsKey(PaginationKey.totals) ? true : null,
                pageByPages);
    }

    private static boolean hasInvalidCombination(Map<PaginationKey, Integer> pageData) {
        return (pageData.containsKey(PaginationKey.size) || pageData.containsKey(PaginationKey.number))
                && (pageData.containsKey(PaginationKey.limit) || pageData.containsKey(PaginationKey.offset));
    }


    /**
     * Default Instance.
     * @param elideSettings general Elide settings
     * @return The default instance.
     */
    public static PaginationImpl getDefaultPagination(Type<?> entityClass, ElideSettings elideSettings) {
        return new PaginationImpl(
                entityClass,
                null,
                null,
                elideSettings.getDefaultPageSize(),
                elideSettings.getDefaultMaxPageSize(),
                null,
                false);
    }

    /**
     * Default Instance.
     * @return The default instance.
     */
    public static PaginationImpl getDefaultPagination(Type<?> entityClass) {
        return new PaginationImpl(
                entityClass,
                null,
                null,
                DEFAULT_PAGE_LIMIT,
                MAX_PAGE_LIMIT,
                null,
                false);
    }
}
