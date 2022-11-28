/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect.jsonapi;

import static com.yahoo.elide.core.type.ClassType.COLLECTION_TYPE;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.jsonapi.parser.JsonApiParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;

/**
 * The default filter dialect supported in Elide 1.0 and 2.0.
 */
public class DefaultFilterDialect implements JoinFilterDialect, SubqueryFilterDialect {
    private final EntityDictionary dictionary;
    public DefaultFilterDialect(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Converts the query parameters to a list of predicates that are then conjoined or organized by type.
     *
     * @param queryParams the query params
     * @return a list of the predicates from the query params
     * @throws ParseException when a filter parameter cannot be parsed
     */
    private List<FilterPredicate> extractPredicates(MultivaluedMap<String, String> queryParams,
                                                    String apiVersion) throws ParseException {
        List<FilterPredicate> filterPredicates = new ArrayList<>();

        Pattern pattern = Pattern.compile("filter\\[([^\\]]+)\\](\\[([^\\]]+)\\])?");
        for (MultivaluedMap.Entry<String, List<String>> entry : queryParams.entrySet()) {
            // Match "filter[<type>.<field>]" OR "filter[<type>.<field>][<operator>]"

            String paramName = entry.getKey();
            List<String> paramValues = entry.getValue();

            Matcher matcher = pattern.matcher(paramName);
            if (!matcher.find()) {
                throw new ParseException("Invalid filter format: " + paramName);
            }

            final String[] keyParts = matcher.group(1).split("\\.");

            if (keyParts.length < 2) {
                throw new ParseException("Invalid filter format: " + paramName);
            }

            final Operator operator = (matcher.group(3) == null) ? Operator.IN
                    : Operator.fromString(matcher.group(3));

            Path path = getPath(keyParts, apiVersion);
            List<Path.PathElement> elements = path.getPathElements();
            Path.PathElement last = elements.get(elements.size() - 1);

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
    public FilterExpression parseGlobalExpression(String path, MultivaluedMap<String, String> filterParams,
                                                  String apiVersion)
            throws ParseException {
        List<FilterPredicate> filterPredicates;
        filterPredicates = extractPredicates(filterParams, apiVersion);

        /* Extract the first collection in the URL */
        String normalizedPath = JsonApiParser.normalizePath(path);
        String[] pathComponents = normalizedPath.split("/");
        String firstPathComponent = "";
        if (pathComponents.length > 0) {
            firstPathComponent = pathComponents[0];
        }

        /* Comma separated filter parameters are joined with logical AND. */
        FilterExpression joinedExpression = null;

        for (FilterPredicate filterPredicate : filterPredicates) {

            Type firstClass = filterPredicate.getPath().getPathElements().get(0).getType();

            /* The first type in the predicate must match the first collection in the URL */
            if (!dictionary.getJsonAliasFor(firstClass).equals(firstPathComponent)) {
                throw new ParseException(String.format("Invalid predicate: %s", filterPredicate));
            }

            if ((filterPredicate.getOperator().equals(Operator.HASMEMBER)
                    || filterPredicate.getOperator().equals(Operator.HASNOMEMBER))
                && !FilterPredicate.isLastPathElementAssignableFrom(
                        dictionary, filterPredicate.getPath(), COLLECTION_TYPE)) {
                throw new ParseException("Invalid Path: Last Path Element has to be a collection type");
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
    public Map<String, FilterExpression> parseTypedExpression(String path, MultivaluedMap<String, String> filterParams,
                                                              String apiVersion)
            throws ParseException {
        Map<String, FilterExpression> expressionMap = new HashMap<>();
        List<FilterPredicate> filterPredicates = extractPredicates(filterParams, apiVersion);

        for (FilterPredicate filterPredicate : filterPredicates) {
            validateFilterPredicate(filterPredicate);
            String entityType = dictionary.getJsonAliasFor(filterPredicate.getEntityType());
            FilterExpression filterExpression = expressionMap.get(entityType);
            if (filterExpression != null) {
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
     * @param apiVersion The client requested version.
     * @return [(author, books), (book, publisher), (publisher, name)]
     * @throws ParseException if the filter cannot be parsed
     */
    private Path getPath(final String[] keyParts, String apiVersion) throws ParseException {
        if (keyParts == null || keyParts.length <= 0) {
            throw new ParseException("Invalid filter expression");
        }

        List<Path.PathElement> path = new ArrayList<>();

        Type<?>[] types = new Type[keyParts.length];
        String type = keyParts[0];

        types[0] = dictionary.getEntityClass(type, apiVersion);

        if (types[0] == null) {
            throw new ParseException("Unknown entity in filter: " + type);
        }

        /* Extract all the paths for the associations */
        for (int i = 1; i < keyParts.length; ++i) {
            final String field = keyParts[i];
            final Type<?> entityClass = types[i - 1];
            final Type<?> fieldType = ("id".equals(field.toLowerCase(Locale.ENGLISH)))
                    ? dictionary.getIdType(entityClass)
                    : dictionary.getParameterizedType(entityClass, field);
            if (fieldType == null) {
                throw new ParseException("Unknown field in filter: " + field);
            }
            types[i] = fieldType;
        }


        /* Build all the Predicate path elements */
        for (int i = 0; i < types.length - 1; ++i) {
            Type typeClass = types[i];
            String fieldName = keyParts[i + 1];
            Type fieldClass = types[i + 1];
            Path.PathElement pathElement = new Path.PathElement(typeClass, fieldClass, fieldName);

            path.add(pathElement);
        }

        return new Path(path);
    }

    /**
     * Check if the relation type in filter predicate is allowed for an operator.
     * Defaults behavior is to prevent filter on toMany relationship.
     * @param filterPredicate
     * @throws ParseException
     */
    private void validateFilterPredicate(FilterPredicate filterPredicate) throws ParseException {
        switch (filterPredicate.getOperator()) {
            case ISEMPTY:
            case NOTEMPTY:
                emptyOperatorConditions(filterPredicate);
                break;
            case HASMEMBER:
            case HASNOMEMBER:
                memberOfOperatorConditions(filterPredicate);
                break;
        }
    }

    /**
     * Check if the predicate has toMany relationship that is not target relationship
     * on which the empty check is performed.
     * @param filterPredicate
     * @throws ParseException
     */
    private void emptyOperatorConditions(FilterPredicate filterPredicate) throws ParseException {
        if (FilterPredicate.toManyInPathExceptLastPathElement(dictionary, filterPredicate.getPath())) {
            throw new ParseException(
                    "Invalid toMany join. toMany association has to be the target collection."
                    + filterPredicate);
        }
    }

    private void memberOfOperatorConditions(FilterPredicate filterPredicate) throws ParseException {
        if (FilterPredicate.toManyInPath(dictionary, filterPredicate.getPath())) {
            if (FilterPredicate.isLastPathElementAssignableFrom(dictionary,
                    filterPredicate.getPath(), COLLECTION_TYPE)) {
                throw new ParseException("Invalid Path: Last Path Element cannot be a collection type");
            }
        } else if (!FilterPredicate.isLastPathElementAssignableFrom(dictionary, filterPredicate.getPath(),
                COLLECTION_TYPE)) {
            throw new ParseException("Invalid Path: Last Path Element has to be a collection type");
        }
    }
}
