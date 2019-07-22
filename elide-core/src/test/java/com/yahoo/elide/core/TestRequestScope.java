/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.security.User;

import javax.ws.rs.core.MultivaluedMap;
import java.util.stream.Collectors;

public class TestRequestScope extends RequestScope {
    public TestRequestScope(String path,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        MultivaluedMap<String, String> queryParams,
                        EntityDictionary dictionary,
                        Class<?> entityClass) {
        super(path, jsonApiDocument, transaction, user, queryParams, new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build());

        setEntityProjection(EntityProjection.builder()
                .type(entityClass)
                .dictionary(dictionary)
                .relationships(
                        dictionary.getRelationships(entityClass).stream()
                                .collect(Collectors.toMap(
                                        (name) -> {
                                            return name;
                                        },
                                        (name) -> {
                                            return EntityProjection.builder()
                                                    .dictionary(dictionary)
                                                    .type(dictionary.getType(entityClass, name))
                                                    .build();
                                        }
                                ))
                )
                .build());
    }


}
