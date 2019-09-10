/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers.state;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidCollectionException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionSubCollectionContext;
import com.yahoo.elide.jsonapi.models.SingleElementSet;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.Set;

/**
 * Record Read State.
 */
public class RecordState extends BaseState {
    private final PersistentResource resource;

    public RecordState(PersistentResource resource) {
        Preconditions.checkNotNull(resource);
        this.resource = resource;
    }

    @Override
    public void handle(StateContext state, SubCollectionReadCollectionContext ctx) {
        String subCollection = ctx.term().getText();
        EntityDictionary dictionary = state.getRequestScope().getDictionary();
        Class<?> entityClass;
        String entityName;
        try {
            RelationshipType type = dictionary.getRelationshipType(resource.getObject(), subCollection);
            if (type == RelationshipType.NONE) {
                throw new InvalidCollectionException(subCollection);
            }
            Class<?> paramType = dictionary.getParameterizedType(resource.getObject(), subCollection);
            if (dictionary.isMappedInterface(paramType)) {
                entityName = EntityDictionary.getSimpleName(paramType);
                entityClass = paramType;
            } else {
                entityName = dictionary.getJsonAliasFor(paramType);
                entityClass = dictionary.getEntityClass(entityName);

            }
            if (entityClass == null) {
                throw new IllegalArgumentException("Unknown type " + entityName);
            }
            final BaseState nextState;
            final CollectionTerminalState collectionTerminalState =
                    new CollectionTerminalState(entityClass, Optional.of(resource), Optional.of(subCollection));
            Set<PersistentResource> collection = null;
            if (type.isToOne()) {
                Optional<FilterExpression> filterExpression =
                        state.getRequestScope().getExpressionForRelation(resource, subCollection);
                collection = resource.getRelationCheckedFiltered(Relationship.builder()
                        .alias(subCollection)
                        .name(subCollection)
                        .projection(EntityProjection.builder()
                                .dictionary(dictionary)
                                .type(entityClass)
                                .filterExpression(filterExpression.orElse(null))
                                .build())
                        .build());
            }
            if (collection instanceof SingleElementSet) {
                PersistentResource record = ((SingleElementSet<PersistentResource>) collection).getValue();
                nextState = new RecordTerminalState(record, collectionTerminalState);
            } else {
                nextState = collectionTerminalState;
            }
            state.setState(nextState);
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(subCollection);
        }
    }

    @Override
    public void handle(StateContext state, SubCollectionReadEntityContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();

        try {
            EntityDictionary dictionary = resource.getDictionary();
            PersistentResource nextRecord = resource.getRelation(Relationship.builder()
                    .alias(subCollection)
                    .name(subCollection)
                    .projection(EntityProjection.builder()
                            .dictionary(dictionary)
                            .type(dictionary.getType(resource.getResourceClass(), subCollection))
                            .build())
                    .build(), id);
            state.setState(new RecordTerminalState(nextRecord));
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(subCollection);
        }
    }

    @Override
    public void handle(StateContext state, SubCollectionSubCollectionContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();
        try {
            EntityDictionary dictionary = resource.getDictionary();
            state.setState(new RecordState(resource.getRelation(Relationship.builder()
                    .alias(subCollection)
                    .name(subCollection)
                    .projection(EntityProjection.builder()
                            .dictionary(dictionary)
                            .type(dictionary.getType(resource.getResourceClass(), subCollection))
                            .build())
                    .build(), id)));
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(subCollection);
        }
    }

    @Override
    public void handle(StateContext state, SubCollectionRelationshipContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();

        PersistentResource childRecord;
        try {
            EntityDictionary dictionary = resource.getDictionary();
            childRecord = resource.getRelation(Relationship.builder()
                    .alias(subCollection)
                    .name(subCollection)
                    .projection(EntityProjection.builder()
                            .dictionary(dictionary)
                            .type(dictionary.getType(resource.getResourceClass(), subCollection))
                            .build())
                    .build(), id);

        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(subCollection);
        }

        String relationName = ctx.relationship().term().getText();
        try {
            EntityDictionary dictionary = resource.getDictionary();

            Optional<FilterExpression> filterExpression =
                        state.getRequestScope().getExpressionForRelation(resource, subCollection);

            childRecord.getRelationCheckedFiltered(Relationship.builder()
                    .alias(relationName)
                    .name(relationName)
                    .projection(EntityProjection.builder()
                            .dictionary(dictionary)
                            .filterExpression(filterExpression.orElse(null))
                            .type(dictionary.getType(childRecord.getResourceClass(), relationName))
                            .build())
                    .build());
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(relationName);
        }

        state.setState(new RelationshipTerminalState(childRecord, relationName));
    }
}
