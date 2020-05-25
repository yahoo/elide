/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.graphql.Entity;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.PersistentResourceFetcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Return values for a Map (i.e. entry set).
 */
public class MapEntryContainer implements GraphQLContainer {
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private final Map.Entry entry;

    /**
     * Constructor.
     *
     * @param entry Entry to contain.
     */
    public MapEntryContainer(Map.Entry entry) {
        this.entry = entry;
    }

    @Override
    public Object processFetch(Environment context, PersistentResourceFetcher fetcher) {
        NonEntityDictionary nonEntityDictionary = fetcher.getNonEntityDictionary();
        String fieldName = context.field.getName();

        Object returnObject;
        if (KEY.equalsIgnoreCase(fieldName)) {
            returnObject = entry.getKey();

        } else if (VALUE.equalsIgnoreCase(fieldName)) {
            returnObject = entry.getValue();
        } else {
            throw new BadRequestException("Invalid field: '" + fieldName
                    + "'. Maps only contain fields 'key' and 'value'");
        }

        if (nonEntityDictionary.hasBinding(returnObject.getClass())) {
            return new NonEntityContainer(returnObject);
        }
        return returnObject;
    }

    /**
     * Converts an attribute which is a list of maps - each containing a KEY and a VALUE
     * into a HashMap with the value of KEY as key and the value of VALUE as the value.
     * @param attribute The attribute to convert.
     * @return The converted map.
     */
    public static Map translateFromGraphQLMap(Entity.Attribute attribute) {
        Map returnMap = new HashMap();
        Object collection = attribute.getValue();

        if (collection == null) {
            return null;
        }

        if (! (collection instanceof Collection)) {
            throw new BadRequestException("Invalid map format for GraphQL request");
        }

        ((Collection) collection).stream().forEach((entry -> {
            if (! (entry instanceof Map)) {
                throw new BadRequestException("Invalid map format for GraphQL request");
            }

            if (! ((Map) entry).containsKey(KEY) && ((Map) entry).containsKey(VALUE)) {
                throw new BadRequestException("Invalid map format for GraphQL request");
            }

            returnMap.put(((Map) entry).get(KEY), ((Map) entry).get(VALUE));
        }));

        return returnMap;
    }
}
