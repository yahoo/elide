/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.strategy;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.AndExpression;
import com.yahoo.elide.core.filter.expression.Expression;
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
 * The default filter strategy supported in Elide 1.0 and 2.0.
 */
public class DefaultFilterStrategy implements JoinFilterStrategy, SubqueryFilterStrategy {
    private final EntityDictionary dictionary;

    public DefaultFilterStrategy(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Coverts the query parameters to a list of predicates that are then conjoined or organized by type.
     * @param queryParams
     * @return
     * @throws ParseException
     */
    private List<Predicate> extractPredicates(MultivaluedMap<String, String> queryParams) throws ParseException {
        List<Predicate> predicates = new ArrayList<>();

        for (MultivaluedMap.Entry<String, List<String>> entry : queryParams.entrySet()) {
            // Match "filter[<type>.<field>]" OR "filter[<type>.<field>][<operator>]"

            String paramName = entry.getKey();
            List<String> paramValues = entry.getValue();

            Matcher matcher = Pattern.compile("filter\\[([^\\]]+)\\](\\[([^\\]]+)\\])?")
                .matcher(paramName);
            if (matcher.find()) {
                final String[] keyParts = matcher.group(1).split("\\.");

                if (keyParts.length < 2) {
                    throw new ParseException("Invalid filter format: " + paramName);
                }

                final Operator operator = (matcher.group(3) == null) ? Operator.IN
                        : Operator.fromString(matcher.group(3));

                List<Predicate.PathElement> path = getPath(keyParts);
                Predicate.PathElement last = path.get(path.size() - 1);

                final List<Object> values = new ArrayList<>();
                for (String valueParams : paramValues) {
                    for (String valueParam : valueParams.split(",")) {
                        values.add(CoerceUtil.coerce(valueParam, last.getFieldType()));
                    }
                }

                Predicate predicate = new Predicate(path, operator, values);

                predicates.add(predicate);
            }
        }

        return predicates;
    }

    @Override
    public Expression parseGlobalExpression(
            String path,
            MultivaluedMap<String, String> queryParams) throws ParseException {
        List<Predicate> predicates;
        predicates = extractPredicates(queryParams);

        /* Extract the first collection in the URL */
        path = Paths.get(path).normalize().toString().replace(File.separatorChar, '/');
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String[] pathComponents = path.split("/");
        String firstPathComponent = "";
        if (pathComponents.length > 0) {
            firstPathComponent = pathComponents[0];
        }

        /* Comma separated filter parameters are joined with logical AND. */
        Expression joinedExpression = null;

        for (Predicate predicate : predicates)  {

            /* The first type in the predicate must match the first collection in the URL */
            if (! predicate.getPath().get(0).getTypeName().equals(firstPathComponent)) {
                throw new ParseException(String.format("Invalid predicate: %s", predicate));
            }

            if (joinedExpression == null) {
                joinedExpression = predicate;
            } else {
                joinedExpression = new AndExpression(joinedExpression, predicate);
            }
        }
        return joinedExpression;
    }

    @Override
    public Map<String, Expression> parseTypedExpression(
            String path,
            MultivaluedMap<String, String> queryParams) throws ParseException {
        Map<String, Expression> expressionMap = new HashMap<>();

        List<Predicate> predicates = extractPredicates(queryParams);

        for (Predicate predicate : predicates) {
            if (predicate.getPath().size() > 1) {
                throw new ParseException("Invalid predicate: " + predicate);
            }

            String entityType = predicate.getEntityType();

            if (expressionMap.containsKey(entityType)) {
                Expression expression = expressionMap.get(entityType);
                expressionMap.put(entityType, new AndExpression(expression, predicate));
            } else {
                expressionMap.put(entityType, predicate);
            }
        }

        return expressionMap;
    }

    /**
     * Parses [ author, books, publisher, name ] into
     * [(author, books), (book, publisher), (publisher, name)]
     * @param keyParts [ author, books, publisher, name ]
     * @return [(author, books), (book, publisher), (publisher, name)]
     * @throws ParseException
     */
    private List<Predicate.PathElement> getPath(final String[] keyParts) throws ParseException {
        if (keyParts == null || keyParts.length <= 0) {
            throw new ParseException("Invalid filter expression");
        }

        List<Predicate.PathElement> path = new ArrayList<>();

        Class<?>[] types = new Class[keyParts.length];
        String type = keyParts[0];
        types[0] = dictionary.getEntityClass(type);

        if (types[0] == null) {
            throw new ParseException("Unknown entity in filter: " + type);
        }

        /* Extract all the paths for the associations */
        for (int i = 1 ; i < keyParts.length ; ++i) {
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
        for (int i = 0 ; i < types.length - 1 ; ++i) {
            Class typeClass = types[i];
            String typeName = dictionary.getJsonAliasFor(types[i]);
            String fieldName = keyParts[i + 1];
            Class fieldClass = types[i + 1];
            Predicate.PathElement pathElement = new Predicate.PathElement(typeClass, typeName, fieldClass, fieldName);

            path.add(pathElement);
        }

        return path;
    }
}
