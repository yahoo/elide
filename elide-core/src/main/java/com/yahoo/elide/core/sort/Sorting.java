/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.sort;

import com.yahoo.elide.core.EntityDictionary;
import lombok.ToString;

import javax.ws.rs.core.MultivaluedMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a simple wrapper around the sort fields from the JSON-API GET Query.
 */
@ToString
public class Sorting {

    /**
     * Denotes the intended sort type from json-api field.
     */
    public enum SortOrder { asc, desc }

    private static final Sorting DEFAULT_EMPTY_INSTANCE = new Sorting(null);

    private final Map<String, SortOrder> sortRules = new LinkedHashMap<>();

    public Sorting(final Map<String, SortOrder> sortingRules) {
        if (sortingRules != null && !sortingRules.isEmpty()) {
            sortRules.putAll(sortingRules);
        }
    }

    public Map<String, SortOrder> getSortRules() {
        return this.sortRules;
    }

    /**
     * Given the sorting rules validate sorting rules against the entities bound to the entityClass
     * @param entityClass  The root class for sorting (eg. /book?sort=-title this would be package.Book)
     * @param dictionary The elide entity dictionary
     * @param <T> The entityClass
     * @return The valid sorting rules - validated through the entity dictionary
     */
    public <T> Map<String, SortOrder> getValidSortingRules(final Class<T> entityClass,
                                                           final EntityDictionary dictionary) {
        final Map<String, SortOrder> validSortRules = new LinkedHashMap<>();
        final List<String> entities = dictionary.getAttributes(entityClass);
        entities.stream()
                .filter(sortRules::containsKey)
                .forEachOrdered(entity -> validSortRules.put(entity, sortRules.get(entity)));
        return validSortRules;
    }

    public boolean isEmpty() {
        return this.sortRules.isEmpty();
    }

    /**
     * Given an entity dictionary and some query params, collect sorting rules.
     * @param queryParams The query params on the request.
     * @return The Sorting instance (default or specific).
     */
    public static Sorting parseQueryParams(final MultivaluedMap<String, String> queryParams) {
        final Map<String, SortOrder> sortingRules = new LinkedHashMap<>();
        queryParams.entrySet().stream()
                .filter(entry -> entry.getKey().equals("sort"))
                .forEachOrdered(entry -> {
                    String sortKey = entry.getValue().get(0);
                    if (sortKey.contains(",")) {
                        for (String sortRule : sortKey.split(",")) {
                            parseSortingRule(sortRule, sortingRules);
                        }
                    } else {
                        parseSortingRule(sortKey, sortingRules);
                    }
                });
        return sortingRules.isEmpty() ? DEFAULT_EMPTY_INSTANCE : new Sorting(sortingRules);
    }

    private static void parseSortingRule(String sortingRule, final Map<String, SortOrder> sortingRules) {
        boolean isDesc = false;
        if (sortingRule.charAt(0) == '-') {
            isDesc = true;
            sortingRule = sortingRule.substring(1);
        }
        sortingRules.put(sortingRule, isDesc ? SortOrder.desc : SortOrder.asc);
    }

    /**
     * Get the default final empty instance.
     * @return The default empty instance.
     */
    public static Sorting getDefaultEmptyInstance() {
        return DEFAULT_EMPTY_INSTANCE;
    }
}
