/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.PersistentResourceFetcher;

import java.util.Map;

import javax.ws.rs.BadRequestException;

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
        String fieldName = context.field.getName();

        if (KEY.equalsIgnoreCase(fieldName)) {
            return entry.getKey();
        } else if (VALUE.equalsIgnoreCase(fieldName)) {
            return entry.getValue();
        }

        throw new BadRequestException("Invalid field: '" + fieldName + "'. Maps only contain fields 'key' and 'value'");
    }
}
