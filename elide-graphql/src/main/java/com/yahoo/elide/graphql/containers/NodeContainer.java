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
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import graphql.language.Field;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

/**
 * Container for nodes.
 */
@AllArgsConstructor
public class NodeContainer implements PersistentResourceContainer, GraphQLContainer {
    @Getter private final PersistentResource persistentResource;

    @Override
    public Object processFetch(Environment context, PersistentResourceFetcher fetcher) {
        EntityDictionary dictionary = context.requestScope.getDictionary();
        Class parentClass = context.parentResource.getResourceClass();
        String fieldName = context.field.getName();
        String idFieldName = dictionary.getIdFieldName(parentClass);

        if (dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
            Attribute requested = context.requestScope.getProjectionInfo()
                    .getAttributeMap().getOrDefault(context.field.getSourceLocation(), null);
            Object attribute = context.parentResource.getAttribute(requested);
            if (attribute instanceof Map) {
                return ((Map<Object, Object>) attribute).entrySet().stream()
                        .map(MapEntryContainer::new)
                        .collect(Collectors.toList());
            }
            return attribute;
        }
        if (dictionary.isRelation(parentClass, fieldName)) { /* fetch relationship properties */
            // get the relationship from constructed projections
            Relationship relationship = context.requestScope
                    .getProjectionInfo()
                    .getRelationshipMap()
                    .getOrDefault(context.field.getSourceLocation(), null);

            if (relationship == null) {
                throw new BadRequestException(
                        "Relationship doesn't have projection " + context.parentResource.getType() + "." + fieldName);
            }

            return fetcher.fetchRelationship(context.parentResource, relationship, context.ids);
        }
        if (Objects.equals(idFieldName, fieldName)) {
            return new DeferredId(context.parentResource);
        }
        throw new BadRequestException("Unrecognized object: " + fieldName + " for: "
                + parentClass.getName() + " in node");
    }
}
