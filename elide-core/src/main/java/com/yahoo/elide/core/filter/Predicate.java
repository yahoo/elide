/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    @Getter @NonNull private String key;
    @Getter @NonNull private Operator operator;
    @Getter @NonNull private String[] values;

    /**
     * Returns field from last element in key.
     *
     * @return field
     */
    public String getField() {
        String[] keys = key.split("\\.");
        return keys[keys.length - 1];
    }

    /**
     * Return collection from the second to last key.
     *
     * @return collection
     */
    public String getCollection() {
        String[] keys = key.split("\\.");
        return keys[keys.length - 2];
    }

    /**
     * Return collection (all but the last element of key).
     *
     * @return lineage
     */
    public List<String> getLineage() {
        String[] keys = key.split("\\.");
        List<String> keyList = new ArrayList<>(Arrays.asList(keys));
        keyList.remove(keyList.size() - 1);
        return keyList;
    }

    public static Set<Predicate> parseQueryParams(final MultivaluedMap<String, String> queryParams) {
        if (queryParams == null) {
            return Collections.emptySet();
        } else {
            final Set<Predicate> predicateSet = new HashSet<>();
            queryParams.entrySet().forEach(queryParameter -> {
                Operator operator;
                // matching "filter[<key>]" OR "filter[<key>][<operator>]"
                final Matcher matcher = Pattern.compile("filter\\[([^\\]]+)\\](\\[([^\\]]+)\\])?")
                        .matcher(queryParameter.getKey());
                if (matcher.find()) {
                    operator = (matcher.group(3) == null) ? Operator.IN : Operator.fromString(matcher.group(3));
                    final String key = matcher.group(1);
                    // split on commas and trim leading/trailing spaces on elements
                    final String[] values = queryParameter.getValue().get(0).split("[\\s]*,[\\s]*");
                    predicateSet.add(new Predicate(key, operator, values));
                }
            });
            return predicateSet;
        }
    }
}
