/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterPredicate.PathElement;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The default filter dialect supported in Elide 1.0 and 2.0.
 */
public class DefaultFilterDialect implements JoinFilterDialect, SubqueryFilterDialect {
    private final EntityDictionary dictionary;

    public DefaultFilterDialect(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Coverts the query parameters to a list of predicates that are then conjoined or organized by type.
     *
     * @param queryParams
     * @return a list of the predicates from the query params
     * @throws ParseException when a filter parameter cannot be parsed
     */
    private List<FilterPredicate> extractPredicates(MultivaluedMap<String, String> queryParams) throws ParseException {
        List<FilterPredicate> filterPredicates = new ArrayList<>();

        for (MultivaluedMap.Entry<String, List<String>> entry : queryParams.entrySet()) {
            // Match "filter[<type>.<field>]" OR "filter[<type>.<field>][<operator>]"

            String paramName = entry.getKey();
            List<String> paramValues = entry.getValue();

            Matcher matcher = Pattern.compile("filter\\[([^\\]]+)\\](\\[([^\\]]+)\\])?").matcher(paramName);
            if (!matcher.find()) {
                throw new ParseException("Invalid filter format: " + paramName);
            }

            final String[] keyParts = matcher.group(1).split("\\.");

            if (keyParts.length < 2) {
                throw new ParseException("Invalid filter format: " + paramName);
            }

            final Operator operator = (matcher.group(3) == null) ? Operator.IN
                    : Operator.fromString(matcher.group(3));

            List<PathElement> path = getPath(keyParts);
            PathElement last = path.get(path.size() - 1);

            final List<Object> values = new ArrayList<>();
            if (operator.isParameterized()) {
                for (String valueParams : paramValues) {
                    for (String valueParam : valueParams.split(",")) {
                        values.add(CoerceUtil.coerce(valueParam, last.getFieldType()));
                    }
                }
            }

            FilterPredicate filterPredicate = new FilterPredicate(path, operator, values);

            filterPredicates.add(filterPredicate);
        }

        return filterPredicates;
    }

    @Override
    public FilterExpression parseGlobalExpression(String path, MultivaluedMap<String, String> filterParams)
            throws ParseException {
        List<FilterPredicate> filterPredicates;
        filterPredicates = extractPredicates(filterParams);

        /* Extract the first collection in the URL */
        String normalizedPath = Paths.get(path).normalize().toString().replace(File.separatorChar, '/');
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        String[] pathComponents = normalizedPath.split("/");
        String firstPathComponent = "";
        if (pathComponents.length > 0) {
            firstPathComponent = pathComponents[0];
        }

        /* Comma separated filter parameters are joined with logical AND. */
        FilterExpression joinedExpression = null;

        for (FilterPredicate filterPredicate : filterPredicates) {

            /* The first type in the predicate must match the first collection in the URL */
            if (!filterPredicate.getPath().get(0).getTypeName().equals(firstPathComponent)) {
                throw new ParseException(String.format("Invalid predicate: %s", filterPredicate));
            }

            if (joinedExpression == null) {
                joinedExpression = filterPredicate;
            } else {
                joinedExpression = new AndFilterExpression(joinedExpression, filterPredicate);
            }
        }
        return joinedExpression;
    }

    @Override
    public Map<String, FilterExpression> parseTypedExpression(String path, MultivaluedMap<String, String> filterParams)
            throws ParseException {
        Map<String, FilterExpression> expressionMap = new HashMap<>();
        List<FilterPredicate> filterPredicates = extractPredicates(filterParams);

        for (FilterPredicate filterPredicate : filterPredicates) {
            if (FilterPredicate.toManyInPath(dictionary, filterPredicate.getPath())) {
                throw new ParseException("Invalid toMany join: " + filterPredicate);
            }

            String entityType = filterPredicate.getRootEntityType();
            if (expressionMap.containsKey(entityType)) {
                FilterExpression filterExpression = expressionMap.get(entityType);
                expressionMap.put(entityType, new AndFilterExpression(filterExpression, filterPredicate));
            } else {
                expressionMap.put(entityType, filterPredicate);
            }
        }

        return expressionMap;
    }

    /**
     * Parses [ author, books, publisher, name ] into [(author, books), (book, publisher), (publisher, name)].
     *
     * @param keyParts [ author, books, publisher, name ]
     * @return [(author, books), (book, publisher), (publisher, name)]
     * @throws ParseException if the filter cannot be parsed
     */
    private List<PathElement> getPath(final String[] keyParts) throws ParseException {
        if (keyParts == null || keyParts.length <= 0) {
            throw new ParseException("Invalid filter expression");
        }

        List<PathElement> path = new ArrayList<>();

        Class<?>[] types = new Class[keyParts.length];
        String type = keyParts[0];
        types[0] = dictionary.getEntityClass(type);

        if (types[0] == null) {
            throw new ParseException("Unknown entity in filter: " + type);
        }

        /* Extract all the paths for the associations */
        for (int i = 1; i < keyParts.length; ++i) {
            final String field = keyParts[i];
            final Class<?> entityClass = types[i - 1];
            final Class<?> fieldType = ("id".equals(field.toLowerCase(Locale.ENGLISH)))
                    ? dictionary.getIdType(entityClass)
                    : dictionary.getParameterizedType(entityClass, field);
            if (fieldType == null) {
                throw new ParseException("Unknown field in filter: " + field);
            }
            types[i] = fieldType;
        }


        /* Build all the Predicate path elements */
        for (int i = 0; i < types.length - 1; ++i) {
            Class typeClass = types[i];
            String typeName = dictionary.getJsonAliasFor(types[i]);
            String fieldName = keyParts[i + 1];
            Class fieldClass = types[i + 1];
            PathElement pathElement = new PathElement(typeClass, typeName, fieldClass, fieldName);

            path.add(pathElement);
        }

        return path;
    }
}
