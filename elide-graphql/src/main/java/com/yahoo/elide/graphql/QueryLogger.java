/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLType;

import java.util.ArrayList;
import java.util.List;

/**
 * Logs an incoming GraphQL query.
 */
public interface QueryLogger {
    /**
     * log current context for debugging.
     * @param operation Current operation
     * @param environment Environment encapsulating graphQL's request environment
     */
    default void logContext(Logger log, RelationshipOp operation, Environment environment) {
        List<?> children = (environment.field.getSelectionSet() != null)
                ? (List) environment.field.getSelectionSet().getChildren()
                : new ArrayList<>();
        List<String> fieldName = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(children)) {
            children.stream().forEach(i -> {
                if (i.getClass().equals(Field.class)) {
                    fieldName.add(((Field) i).getName());
                } else if (i.getClass().equals(FragmentSpread.class)) {
                    fieldName.add(((FragmentSpread) i).getName());
                } else {
                    log.debug("A new type of Selection, other than Field and FragmentSpread was encountered, {}",
                            i.getClass());
                }
            });
        }

        String requestedFields = environment.field.getName() + fieldName;

        GraphQLType parent = environment.parentType;
        if (log.isDebugEnabled()) {
            String typeName = (parent instanceof GraphQLNamedType)
                    ? ((GraphQLNamedType) parent).getName()
                    : parent.toString();
            log.debug("{} {} fields with parent {}<{}>", operation, requestedFields,
                    EntityDictionary.getSimpleName(EntityDictionary.getType(parent)), typeName);
        }
    }
}
