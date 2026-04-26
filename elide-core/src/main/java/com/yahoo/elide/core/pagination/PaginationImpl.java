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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Holds state associated with pagination.
 */
@ToString
@EqualsAndHashCode
public class PaginationImpl implements Pagination {
    /**
     * Denotes the internal field names for paging.
     */
    public enum PaginationKey {
        offset, number, size, limit, totals, first, after, last, before
    }

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

    // For cursor pagination with direction forward
    public static final String PAGE_FIRST_KEY = "page[first]";

    // For cursor pagination with direction forward
    public static final String PAGE_AFTER_KEY = "page[after]";

    // For cursor pagination with direction backward
    public static final String PAGE_LAST_KEY = "page[last]";

    // For cursor pagination with direction backward
    public static final String PAGE_BEFORE_KEY = "page[before]";

    public static final Map<String, PaginationKey> PAGE_KEYS = ImmutableMap.of(
            PAGE_NUMBER_KEY, PaginationKey.number,
            PAGE_SIZE_KEY, PaginationKey.size,
            PAGE_OFFSET_KEY, PaginationKey.offset,
            PAGE_LIMIT_KEY, PaginationKey.limit,
            PAGE_TOTALS_KEY, PaginationKey.totals,
            PAGE_FIRST_KEY, PaginationKey.first,
            PAGE_AFTER_KEY, PaginationKey.after,
            PAGE_LAST_KEY, PaginationKey.last,
            PAGE_BEFORE_KEY, PaginationKey.before);

    @Getter
    @Setter
    private Long pageTotals = null; // By default this is null and must be explicitly set

    private static final String PAGE_KEYS_CSV = PAGE_KEYS.keySet().stream().collect(Collectors.joining(", "));

    @Getter
    private final int offset;

    @Getter
    private final int limit;

    @Getter
    private final String before;

    @Getter
    private final String after;

    @Getter
    private final Direction direction;

    @Getter
    @Setter
    private String startCursor;

    @Getter
    @Setter
    private String endCursor;

    @Getter
    @Setter
    private Boolean hasPreviousPage;

    @Getter
    @Setter
    private Boolean hasNextPage;

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
                systemDefaultLimit, systemMaxLimit, generateTotals, pageByPages, null, null, null);
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
        this(entityClass, clientOffset, clientLimit,
                systemDefaultLimit, systemMaxLimit, generateTotals, pageByPages, null, null, null);
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
     * @param before The cursor for cursor pagination.
     * @param after The cursor for cursor pagination.
     * @param direction The direction for cursor pagination.
     */
    public PaginationImpl(Class<?> entityClass,
                          Integer clientOffset,
                          Integer clientLimit,
                          int systemDefaultLimit,
                          int systemMaxLimit,
                          Boolean generateTotals,
                          Boolean pageByPages,
                          String before,
                          String after,
                          Direction direction) {
        this(ClassType.of(entityClass), clientOffset, clientLimit,
                systemDefaultLimit, systemMaxLimit, generateTotals, pageByPages, before, after, direction);
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
     * @param before The cursor for cursor pagination.
     * @param after The cursor for cursor pagination.
     * @param direction The direction for cursor pagination.
     */
    public PaginationImpl(Type<?> entityClass,
                           Integer clientOffset,
                           Integer clientLimit,
                           int systemDefaultLimit,
                           int systemMaxLimit,
                           Boolean generateTotals,
                           Boolean pageByPages,
                           String before,
                           String after,
                           Direction direction) {
        this.entityClass = entityClass;
        this.defaultInstance = (clientOffset == null && clientLimit == null && generateTotals == null
                && before == null && after == null);

        Paginate paginate = entityClass != null ? (Paginate) entityClass.getAnnotation(Paginate.class) : null;

        this.limit = clientLimit != null
                ? clientLimit
                : (paginate != null ? paginate.defaultPageSize() : systemDefaultLimit);

        int maxLimit = paginate != null ? paginate.maxPageSize() : systemMaxLimit;

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

        this.direction = direction;
        this.before = before;
        this.after = after;

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
                                                  final Map<String, List<String>> queryParams,
                                                  ElideSettings elideSettings)
            throws InvalidValueException {

        if (queryParams.isEmpty()) {
            return getDefaultPagination(entityClass, elideSettings);
        }

        final Map<PaginationKey, Integer> pageData = new HashMap<>();
        String before = null;
        String after = null;
        Direction direction = null;

        for (Entry<String, List<String>> paramEntry : queryParams.entrySet()) {
            final String queryParamKey = paramEntry.getKey();
            if (PAGE_KEYS.containsKey(queryParamKey)) {
                PaginationKey paginationKey = PAGE_KEYS.get(queryParamKey);
                if (paginationKey.equals(PaginationKey.totals)) {
                    // page[totals] is a valueless parameter, use value of 0 just so that its presence can
                    // be recorded in the map
                    pageData.put(paginationKey, 0);
                } else if (paginationKey.equals(PaginationKey.before)) {
                    final String value = paramEntry.getValue().get(0);
                    before = value;
                    direction = Direction.BACKWARD;
                } else if (paginationKey.equals(PaginationKey.after)) {
                    final String value = paramEntry.getValue().get(0);
                    after = value;
                    direction = Direction.FORWARD;
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
                throw new InvalidValueException(
                        "Invalid Pagination Parameter. Accepted values are " + PAGE_KEYS_CSV);
            }
        }
        if (before != null && after != null) {
            direction = Direction.BETWEEN;
        }
        return getPagination(entityClass, pageData, before, after, direction, elideSettings);
    }


    /**
     * Construct a pagination object from page data and elide settings.
     *
     * @param entityClass The collection type.
     * @param pageData Map containing pagination information
     * @param before the cursor
     * @param after the cursor
     * @param direction the cursor direction
     * @param elideSettings Settings containing pagination defaults
     * @return Pagination object
     */
    private static PaginationImpl getPagination(Type<?> entityClass, Map<PaginationKey, Integer> pageData,
            String before, String after, Direction direction, ElideSettings elideSettings) {
        if (hasInvalidCombination(pageData)) {
            throw new InvalidValueException("Invalid usage of pagination parameters.");
        }
        if (pageData.containsKey(PaginationKey.first) && pageData.containsKey(PaginationKey.last)) {
            throw new InvalidValueException("page[first] and page[last] cannot be used together.");
        }
        if (pageData.containsKey(PaginationKey.first) && direction != null) {
            throw new InvalidValueException("page[first] cannot be used together with page[before] or page[after].");
        }
        if (pageData.containsKey(PaginationKey.last) && direction != null) {
            throw new InvalidValueException("page[last] cannot be used together with page[before] or page[after].");
        }

        boolean pageByPages = false;
        Integer offset = pageData.getOrDefault(PaginationKey.offset, null);
        Integer limit = pageData.getOrDefault(PaginationKey.limit, null);

        if (pageData.containsKey(PaginationKey.size) || pageData.containsKey(PaginationKey.number)) {
            pageByPages = true;
            offset = pageData.getOrDefault(PaginationKey.number, null);
            limit = pageData.getOrDefault(PaginationKey.size, null);
        }

        // Cursor
        if (pageData.containsKey(PaginationKey.first)) {
            direction = Direction.FORWARD;
            limit = pageData.getOrDefault(PaginationKey.first, null);
        } else if (pageData.containsKey(PaginationKey.last)) {
            direction = Direction.BACKWARD;
            limit = pageData.getOrDefault(PaginationKey.last, null);
        }

        return new PaginationImpl(entityClass,
                offset,
                limit,
                elideSettings.getDefaultPageSize(),
                elideSettings.getMaxPageSize(),
                pageData.containsKey(PaginationKey.totals) ? true : null,
                pageByPages,
                before,
                after,
                direction);
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
                elideSettings.getMaxPageSize(),
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
                DEFAULT_PAGE_SIZE,
                MAX_PAGE_SIZE,
                null,
                false);
    }
}
