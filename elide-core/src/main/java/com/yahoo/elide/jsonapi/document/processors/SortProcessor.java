/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.document.processors;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Implementation for JSON API 'sort' query param.
 *
 * Sorts a JsonApiDocument's data field based on attributes specified in query param.
 * If an attribute is not found, it will be ignored for sorting.
 * Null values are sorted first.
 *
 * Supports sort by multiple fields by separating them with ','
 * Supports descending sorts by prefixing the field with '-'
 * Does not support sorting by a relationship's field (ex: author.name)
 *
 * Example:
 *  /posts?sort=title,-created
 */
public class SortProcessor implements DocumentProcessor {

    /**
     * The constant SORT_PARAM.
     */
    public static final String SORT_PARAM = "sort";

    /**
     * The constant DESCENDING_TOKEN.
     */
    public static final char DESCENDING_TOKEN = '-';

    /**
     * Sorts a JsonApiDocument's data field based on attributes specified in the 'sort' query param.
     * This method is a no-op as a single record is already sorted.
     *
     * @param jsonApiDocument the json api document
     * @param resource the resource
     * @param queryParams the query params
     */
    @Override
    public void execute(JsonApiDocument jsonApiDocument, PersistentResource resource,
                        Optional<MultivaluedMap<String, String>> queryParams) {
        // NO-OP
    }

    /**
     * Sorts a JsonApiDocument's data field based on attributes specified in the 'sort' query param.
     *
     * @param jsonApiDocument the json api document
     * @param resources the resources
     * @param queryParams the query params
     */
    @Override
    public void execute(JsonApiDocument jsonApiDocument, Set<PersistentResource> resources,
                        Optional<MultivaluedMap<String, String>> queryParams) {

        // Only sort if requested by query param
        queryParams.filter(params -> params.containsKey(SORT_PARAM)).ifPresent(params -> {
            List<String> sortFields = params.get(SORT_PARAM);

            // Sort the json api document's data property based on the requested sort fields
            sort(jsonApiDocument.getData(), sortFields);
        });

    }

    /**
     * Sort data based on provided sort field list.
     *
     * @param data resource data to sort
     * @param sortFields - attribute fields within the data to sort by
     */
    private void sort(Data<Resource> data, List<String> sortFields) {
        List<Comparator<Resource>> comparisonFunctions = buildComparisonFunctions(sortFields);

        data.sort((a, b) ->

            // Apply comparison functions in order until one returns a non-zero value
            Ordering.compound(comparisonFunctions).compare(a, b)
        );
    }

    /**
     * Build list of comparators.
     *
     * @param sortFields attribute fields within the data to sort by
     * @return list of comparators to do the sorting
     */
    private List<Comparator<Resource>> buildComparisonFunctions(List<String> sortFields) {
        // Convert sort fields to comparisons
        return sortFields.stream()
                .map(this::comparisonForField)
                .collect(Collectors.toList());
    }

    /**
     * Field comparator.
     *
     * @param field name of attribute field to compare, prefix with '-' to specify descending order
     * @return a comparison between resources for the given attribute field
     */
    private Comparator<Resource> comparisonForField(String field) {
        // Determine if ascending or descending
        if (field.charAt(0) == DESCENDING_TOKEN) {
            // Remove descending token to get field name
            String parsedField = field.substring(1);
            return Ordering.from(attributeComparison(parsedField)).nullsFirst().reverse();
        } else {
            return Ordering.from(attributeComparison(field)).nullsFirst();
        }
    }

    /**
     * Attribute comparator.
     *
     * @param field - name of attribute field to compare
     * @return a comparison between resources for the given attribute field
     */
    private Comparator<Resource> attributeComparison(String field) {
        return (a, b) ->
            // Compare requested attribute using an object comparison
            Ordering.from(this::compareObjects)
                .nullsFirst()
                .compare(
                        a.getAttributes().get(field),
                        b.getAttributes().get(field)
                );
    }

    /**
     * Compare objects.
     *
     * @param a first object to compare
     * @param b second object to compare
     * @return negative number if a < b
     *         positive number if b > a
     *         0 if a == b
     *         0 if a comparison for the given object class cannot be made
     */
    private int compareObjects(Object a, Object b) {
        if (a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }

        // Can't compare objects without Comparable implementation
        return 0;
    }
}
