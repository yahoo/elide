/*
 * Copyright 2020, Verizon Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.PersistentResourceFetcher;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Container for models not managed by Elide.
 */
@AllArgsConstructor
public class NonEntityContainer implements GraphQLContainer {
    @Getter private final Object nonEntity;

    @Override
    public Object processFetch(Environment context, PersistentResourceFetcher fetcher) {
        NonEntityDictionary nonEntityDictionary = fetcher.getNonEntityDictionary();

        String fieldName = context.field.getName();

        //There is no Elide security for models not managed by Elide.
        Object object = nonEntityDictionary.getValue(nonEntity, fieldName, context.requestScope);

        if (object == null) {
            return null;
        }

        if (nonEntityDictionary.hasBinding(object.getClass())) {
            return new NonEntityContainer(object);
        }

        if (object instanceof Map) {
            return ((Map<Object, Object>) object).entrySet().stream()
                    .map(MapEntryContainer::new)
                    .collect(Collectors.toList());
        }

        if (object instanceof Collection) {
            Class<?> innerType = nonEntityDictionary.getParameterizedType(nonEntity.getClass(), fieldName);

            if (nonEntityDictionary.hasBinding(innerType)) {
                return ((Collection) object).stream()
                        .map(NonEntityContainer::new)
                        .collect(Collectors.toList());
            }
        }

        return object;
    }
}
