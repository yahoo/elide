/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.sort;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidValueException;

import lombok.ToString;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Generates a simple wrapper around the sort fields from the JSON-API GET Query.
 */
@ToString
public class Sorting {

    /**
     * Denotes the intended sort type from json-api field.
     */
    public enum SortOrder { asc, desc }

    private final Map<String, SortOrder> sortRules = new LinkedHashMap<>();
    private static final Sorting DEFAULT_EMPTY_INSTANCE = new Sorting(null);
    private static final String JSONAPI_ID_KEYWORD = "id";

    /**
     * Constructs a new Sorting instance.
     * @param sortingRules The map of sorting rules
     */
    public Sorting(final Map<String, SortOrder> sortingRules) {
        if (sortingRules != null) {
            sortRules.putAll(sortingRules);
        }
    }

    /**
     * Given the sorting rules validate sorting rules against the entities bound to the entityClass.
     * @param entityClass  The root class for sorting (eg. /book?sort=-title this would be package.Book)
     * @param dictionary The elide entity dictionary
     * @param <T> The entityClass
     * @return The valid sorting rules - validated through the entity dictionary, or empty dictionary
     * @throws InvalidValueException when sorting values are not valid for the jpa entity
     */
    public <T> Map<Path, SortOrder> getValidSortingRules(final Class<T> entityClass,
                                                         final EntityDictionary dictionary)
            throws InvalidValueException {
        Map<Path, SortOrder> returnMap = new LinkedHashMap<>();
        for (Map.Entry<String, SortOrder> entry : replaceIdRule(dictionary.getIdFieldName(entityClass)).entrySet()) {
            String dotSeparatedPath = entry.getKey();
            SortOrder order = entry.getValue();

            //Creating a path validates that the dot separated path is valid.
            Path path = new Path(entityClass, dictionary, dotSeparatedPath);

            if (! isValidSortRulePath(path, dictionary)) {
                throw new InvalidValueException("Cannot sort across a to-many relationship: " + path.getFieldPath());
            }

            returnMap.put(path, order);
        }

        return returnMap;
    }

    /**
     * Validates that none of the provided path's relationships are to-many.
     * @param path The path to validate
     * @param dictionary The elide entity dictionary
     * @return True if the path is valid. False otherwise.
     */
    protected static boolean isValidSortRulePath(Path path, EntityDictionary dictionary) {
        //Validate that none of the relationships are to-many
        for (Path.PathElement pathElement : path.getPathElements()) {
            if (! dictionary.isRelation(pathElement.getType(), pathElement.getFieldName())) {
                continue;
            }

            if (dictionary.getRelationshipType(pathElement.getType(), pathElement.getFieldName()).isToMany()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Informs if the structure is default instance.
     * @return true if this instance is empty - no sorting rules
     */
    public boolean isDefaultInstance() {
        return this.sortRules.isEmpty();
    }

    /**
     * Given the query params on the GET request, collect possible sorting rules.
     * @param queryParams The query params on the request.
     * @return The Sorting instance (default or specific).
     */
    public static Sorting parseQueryParams(final MultivaluedMap<String, String> queryParams) {
        List<String> sortRules = queryParams.entrySet().stream()
                .filter(entry -> entry.getKey().equals("sort"))
                .map(entry -> entry.getValue().get(0))
                .collect(Collectors.toList());
        return parseSortRules(sortRules);
    }

    /**
     * Parse a raw sort rule.
     * @param sortRule Sorting string to parse
     * @return Sorting object.
     */
    public static Sorting parseSortRule(String sortRule) {
        return parseSortRules(Arrays.asList(sortRule));
    }

    /**
     * Internal helper to parse list of sorting rules.
     * @param sortRules Sorting rules to parse
     * @return Sorting object containing parsed sort rules
     */
    private static Sorting parseSortRules(List<String> sortRules) {
        final Map<String, SortOrder> sortingRules = new LinkedHashMap<>();
        for (String sortRule : sortRules) {
            if (sortRule.contains(",")) {
                for (String sortRuleSplit : sortRule.split(",")) {
                    parseSortRule(sortRuleSplit, sortingRules);
                }
            } else {
                parseSortRule(sortRule, sortingRules);
            }
        }
        return sortingRules.isEmpty() ? DEFAULT_EMPTY_INSTANCE : new Sorting(sortingRules);
    }

    /**
     * Internal helper method to parse sorting rule strings.
     * @param sortRule The string from the queryParams
     * @param sortingRules The final shared reference to the sortingRules map
     */
    private static void parseSortRule(String sortRule, final Map<String, SortOrder> sortingRules) {
        boolean isDesc = false;
        char firstCharacter = sortRule.charAt(0);
        if (firstCharacter == '-') {
            isDesc = true;
            sortRule = sortRule.substring(1);
        }
        if (firstCharacter == '+') {
            // json-api spec supports asc by default, there is no need to explicitly support +
            sortRule = sortRule.substring(1);
        }
        sortingRules.put(sortRule, isDesc ? SortOrder.desc : SortOrder.asc);
    }

    /**
     * Replace id with proper field for object.
     *
     * @param idFieldName Name of the object's id field.
     * @return Sort rules with id field name replaced
     */
    private LinkedHashMap<String, SortOrder> replaceIdRule(String idFieldName) {
        LinkedHashMap<String, SortOrder> result = new LinkedHashMap<>();
        for (Map.Entry<String, SortOrder> entry : sortRules.entrySet()) {
            String key = entry.getKey();
            SortOrder value = entry.getValue();
            if (JSONAPI_ID_KEYWORD.equals(key)) {
                result.put(idFieldName, value);
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Get the default final empty instance.
     * @return The default empty instance.
     */
    public static Sorting getDefaultEmptyInstance() {
        return DEFAULT_EMPTY_INSTANCE;
    }
}
