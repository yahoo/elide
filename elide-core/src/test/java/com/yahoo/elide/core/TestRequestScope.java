/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.EntityProjection.EntityProjectionBuilder;
import com.yahoo.elide.security.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility subclass that helps construct RequestScope objects for testing.
 */
public class TestRequestScope extends RequestScope {
    public TestRequestScope(DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        Class<?> entityClass,
                        int nestLevel) {
        super(null, new JsonApiDocument(), transaction, user, null,
                new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build());

        /* Create an entity projection from a top level object and all of its relationships */
        setEntityProjection(buildEntityProjection(entityClass, nestLevel));
    }

    EntityProjection buildEntityProjection(Class<?> entityClass, int nestLevel) {

        /* Create an entity projection from a top level object and all of its relationships */
        EntityProjectionBuilder builder = EntityProjection.builder()
                .type(entityClass)
                .dictionary(dictionary);

        List<String> relationships =  dictionary.getRelationships(entityClass);
        if (nestLevel > 0  && relationships != null && !relationships.isEmpty()) {
            builder.relationships(
                dictionary.getRelationships(entityClass).stream()
                    .collect(Collectors.toMap(
                        (name) -> {
                            return name;
                        },
                        (name) -> {
                            return buildEntityProjection(dictionary.getParameterizedType(entityClass, name), nestLevel - 1);
                        }
                    ))
            );
        }

        return builder.build();
    }
}
