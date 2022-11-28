/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.graphql.DeferredId;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.PersistentResourceFetcher;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Container for nodes.
 */
@AllArgsConstructor
public class NodeContainer implements PersistentResourceContainer, GraphQLContainer {
    @Getter protected final PersistentResource persistentResource;

    @Override
    public Object processFetch(Environment context) {
        EntityDictionary entityDictionary = context.requestScope.getDictionary();
        NonEntityDictionary nonEntityDictionary = context.nonEntityDictionary;

        Type parentClass = context.parentResource.getResourceType();
        String fieldName = context.field.getName();
        String idFieldName = entityDictionary.getIdFieldName(parentClass);

        if (entityDictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
            Attribute requested = context.requestScope.getProjectionInfo()
                    .getAttributeMap().getOrDefault(context.field.getSourceLocation(), null);
            Object attribute = context.parentResource.getAttribute(requested);

            if (attribute != null && nonEntityDictionary.hasBinding(EntityDictionary.getType(attribute))) {
                return new NonEntityContainer(attribute);
            }

            if (attribute instanceof Map) {
                return ((Map<Object, Object>) attribute).entrySet().stream()
                        .map(MapEntryContainer::new)
                        .collect(Collectors.toList());
            }

            if (attribute instanceof Collection) {
                Type<?> innerType = entityDictionary.getParameterizedType(parentClass, fieldName);

                if (nonEntityDictionary.hasBinding(innerType)) {
                    return ((Collection) attribute).stream()
                            .map(NonEntityContainer::new)
                            .collect(Collectors.toList());
                }
            }

            return attribute;
        }
        if (entityDictionary.isRelation(parentClass, fieldName)) { /* fetch relationship properties */
            // get the relationship from constructed projections
            Relationship relationship = context.requestScope
                    .getProjectionInfo()
                    .getRelationshipMap()
                    .getOrDefault(context.field.getSourceLocation(), null);

            if (relationship == null) {
                throw new BadRequestException("Relationship doesn't have projection "
                                + context.parentResource.getTypeName() + "." + fieldName);
            }

            return fetchRelationship(context, relationship);
        }
        if (Objects.equals(idFieldName, fieldName)) {
            return new DeferredId(context.parentResource);
        }
        throw new BadRequestException("Unrecognized object: " + fieldName + " for: "
                + parentClass.getName() + " in node");
    }

    protected Object fetchRelationship(Environment context, Relationship relationship) {
        return PersistentResourceFetcher.fetchRelationship(context.parentResource, relationship, context.ids);
    }
}
