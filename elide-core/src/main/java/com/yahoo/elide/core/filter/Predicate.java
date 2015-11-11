/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.utils.coerse.CoerceUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Predicate class.
 */
@AllArgsConstructor
@ToString
public class Predicate {
    @Getter @NonNull private String type;
    @Getter @NonNull private String field;
    @Getter @NonNull private Operator operator;
    @Getter @NonNull private List<Object> values;

    public static Set<Predicate> parseQueryParams(final EntityDictionary dictionary,
                                                  final MultivaluedMap<String, String> queryParams) {
        final Set<Predicate> predicateSet = new HashSet<>();

        queryParams.entrySet().forEach(queryParameter -> {
            // Match "filter[<type>.<field>]" OR "filter[<type>.<field>][<operator>]"
            Matcher matcher = Pattern.compile("filter\\[([^\\]]+)\\](\\[([^\\]]+)\\])?")
                    .matcher(queryParameter.getKey());
            if (matcher.find()) {
                final String[] keyParts = matcher.group(1).split("\\.");

                if (keyParts.length != 2) {
                    throw new InvalidPredicateException("Invalid filter format: " + queryParameter.getKey());
                }

                final String type = keyParts[0];
                final String field = keyParts[1];

                final Operator operator = (matcher.group(3) == null) ? Operator.IN
                        : Operator.fromString(matcher.group(3));

                final Class<?> entityClass = dictionary.getBinding(type);
                if (entityClass == null) {
                    throw new InvalidPredicateException("Unknown entity in filter: " + type);
                }

                final Class<?> fieldType = dictionary.getParameterizedType(entityClass, field);
                if (fieldType == null) {
                    throw new InvalidPredicateException("Unknown field in filter: " + field);
                }

                final List<Object> values = new ArrayList<>();
                for (String valueParams : queryParameter.getValue()) {
                    for (String valueParam : valueParams.split(",")) {
                        values.add(CoerceUtil.coerce(valueParam, fieldType));
                    }
                }

                predicateSet.add(new Predicate(type, field, operator, values));
            }
        });

        return predicateSet;
    }
}
