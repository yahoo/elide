/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.graphql.DeferredId;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.PersistentResourceFetcher;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.ws.rs.BadRequestException;
import java.util.Objects;

import static com.yahoo.elide.graphql.PersistentResourceFetcher.requestContainsPageInfo;

/**
 * Container for nodes.
 */
@AllArgsConstructor
public class NodeContainer implements PersistentResourceContainer, GraphQLContainer {
    @Getter private final PersistentResource persistentResource;

    @Override
    public Object process(Environment context, PersistentResourceFetcher fetcher) {
        EntityDictionary dictionary = context.requestScope.getDictionary();
        Class parentClass = context.parentResource.getResourceClass();
        String fieldName = context.field.getName();
        String idFieldName = dictionary.getIdFieldName(parentClass);

        if (dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
            return context.parentResource.getAttribute(fieldName);
        }
        if (dictionary.isRelation(parentClass, fieldName)) { /* fetch relationship properties */
            String entityType = dictionary.getJsonAliasFor(dictionary.getParameterizedType(parentClass, fieldName));
            boolean generateTotals = requestContainsPageInfo(entityType, context.field);
            return fetcher.fetchRelationship(context, context.parentResource,
                    fieldName, context.ids, context.offset, context.first, context.sort, context.filters,
                    generateTotals);
        }
        if (Objects.equals(idFieldName, fieldName)) {
            return new DeferredId(context.parentResource);
        }
        throw new BadRequestException("Unrecognized object: " + fieldName + " for: "
                + parentClass.getName() + " in node");
    }
}
