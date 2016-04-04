/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Predicate class.
 */
@AllArgsConstructor
@ToString
public class Predicate {
    @Getter @NonNull private String field;
    @Getter @NonNull private Operator operator;
    @Getter @NonNull private List<Object> values;

    public static Map<String, Set<Predicate>> parseQueryParams(final EntityDictionary dictionary,
                                                               final MultivaluedMap<String, String> queryParams) {
        final Map<String, Set<Predicate>> predicates = new HashMap<>();

        queryParams.entrySet().forEach(queryParameter -> {
            // Match "filter[<type>.<field>]" OR "filter[<type>.<field>][<operator>]"
            Matcher matcher = Pattern.compile("filter\\[([^\\]]+)\\](\\[([^\\]]+)\\])?")
                    .matcher(queryParameter.getKey());
            if (matcher.find()) {
                final String[] keyParts = matcher.group(1).split("\\.");

                if (keyParts.length < 2) {
                    throw new InvalidPredicateException("Invalid filter format: " + queryParameter.getKey());
                }


                final Operator operator = (matcher.group(3) == null) ? Operator.IN
                        : Operator.fromString(matcher.group(3));

                final Class<?>[] types = getTypes(keyParts, dictionary);
                final String type = getType(keyParts);
                final String field = getField(keyParts);
                final Class<?> fieldType = getFieldType(types);

                final List<Object> values = new ArrayList<>();
                for (String valueParams : queryParameter.getValue()) {
                    for (String valueParam : valueParams.split(",")) {
                        values.add(CoerceUtil.coerce(valueParam, fieldType));
                    }
                }

                if (!predicates.containsKey(type)) {
                    predicates.put(type, new LinkedHashSet<>());
                }
                predicates.get(type).add(new Predicate(field, operator, values));
            }
        });

        return predicates;
    }

    /**
     * Retrieve the type of the object to be filtered.
     *
     * NOTE: Types are now fully qualified by (ROOT_TYPE.FIELD1.FIELD2..FIELD(N-1)).ACCESSED_FIELD
     *
     * @param types Array of types
     * @return Type of object to be filtered. Empty string if nothing found.
     */
    private static String getType(final String[] types) {
        if (types == null || types.length <= 1) {
            return "";
        }
        return String.join(".", Arrays.copyOf(types, types.length - 1));
    }

    /**
     * Retrieve the type of the field being accessed.
     *
     * @param types Array of types
     * @return Type of the field
     */
    private static Class<?> getFieldType(final Class<?>[] types) {
        if (types == null || types.length <= 1) {
            throw new InvalidPredicateException("Unknown type for field");
        }
        return types[types.length - 1];
    }

    /**
     * Get the classes of each field contained within the key part.
     *
     * NOTE: This method checks that the specified traversal path to a particular type
     *       is valid. In the case of an invalid field, it will throw an "InvalidPredicateException."
     *
     * @param keyParts Key components
     * @param dictionary Entity dictionary
     * @return An array containing types for each of the key parts (in order). Empty array if none found.
     */
    private static Class<?>[] getTypes(final String[] keyParts, final EntityDictionary dictionary) {
        if (keyParts == null || keyParts.length <= 0) {
            return new Class[0];
        }
        Class<?>[] types = new Class[keyParts.length];
        String type = keyParts[0];
        types[0] = dictionary.getEntityClass(type);
        if (types[0] == null) {
            throw new InvalidPredicateException("Unknown entity in filter: " + type);
        }
        for (int i = 1 ; i < keyParts.length ; ++i) {
            final String field = keyParts[i];
            final Class<?> entityClass = types[i - 1];
            final Class<?> fieldType = ("id".equals(field.toLowerCase(Locale.ENGLISH)))
                    ? dictionary.getIdType(entityClass)
                    : dictionary.getParameterizedType(entityClass, field);
            if (fieldType == null) {
                throw new InvalidPredicateException("Unknown field in filter: " + field);
            }
            types[i] = fieldType;
        }
        return types;
    }

    /**
     * Retrieve the string name for the final field being accessed in a list of keyParts.
     *
     * @param keyParts Key components
     * @return Final field being accessed or empty string if no key parts
     */
    private static String getField(final String[] keyParts) {
        if (keyParts != null && keyParts.length > 0) {
            return keyParts[keyParts.length - 1];
        }
        return "";
    }
}
