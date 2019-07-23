/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidCollectionException;
import com.yahoo.elide.generated.parsers.CoreBaseVisitor;
import com.yahoo.elide.generated.parsers.CoreParser;
import com.yahoo.elide.request.EntityProjection;

import javax.ws.rs.core.MultivaluedMap;

public class EntityProjectionMaker extends CoreBaseVisitor<EntityProjection> {
    private EntityDictionary dictionary;

    public EntityProjectionMaker(EntityDictionary dictionary, MultivaluedMap<String, String> headers) {
        this.dictionary = dictionary;
    }

    @Override
    public EntityProjection visitRootCollectionLoadEntities(CoreParser.RootCollectionLoadEntitiesContext ctx) {
        return visitTerminalCollection(ctx.term());
    }

    @Override
    public EntityProjection visitSubCollectionReadCollection(CoreParser.SubCollectionReadCollectionContext ctx) {
        return visitTerminalCollection(ctx.term());
    }


    @Override
    public EntityProjection visitSubCollectionReadEntity(CoreParser.SubCollectionReadEntityContext ctx) {
        return super.visitSubCollectionReadEntity(ctx);
    }

    @Override
    public EntityProjection visitRootCollectionSubCollection(CoreParser.RootCollectionSubCollectionContext ctx) {
        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection());
    }

    @Override
    public EntityProjection visitRootCollectionRelationship(CoreParser.RootCollectionRelationshipContext ctx) {
        return visitEntityWithRelationship(ctx.entity(), ctx.relationship());
    }

    @Override
    public EntityProjection visitSubCollectionRelationship(CoreParser.SubCollectionRelationshipContext ctx) {
        return visitEntityWithRelationship(ctx.entity(), ctx.relationship());
    }

    @Override
    public EntityProjection visitEntity(CoreParser.EntityContext ctx) {
        String entityName = ctx.term().getText();
        String id = ctx.id().getText();

        Class<?> entityClass = dictionary.getEntityClass(entityName);
        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .build();
    }

    @Override
    public EntityProjection visitRelationship(CoreParser.RelationshipContext ctx) {
        String entityName = ctx.term().getText();

        Class<?> entityClass = dictionary.getEntityClass(entityName);
        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .build();
    }

    @Override
    public EntityProjection visitSubCollectionSubCollection(CoreParser.SubCollectionSubCollectionContext ctx) {
        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection());
    }


    private EntityProjection visitEntityWithSubCollection(CoreParser.EntityContext entity,
                                                          CoreParser.SubCollectionContext subCollection) {

        String entityName = entity.term().getText();
        String id = entity.id().getText();

        Class<?> entityClass = dictionary.getEntityClass(entityName);
        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        String relationshipName = subCollection.getChild(0).getText();
        EntityProjection relationship = subCollection.accept(this);

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .relationship(relationshipName, relationship)
                .build();
    }

    private EntityProjection visitEntityWithRelationship(CoreParser.EntityContext entity,
                                                         CoreParser.RelationshipContext relationship) {
        String entityName = entity.term().getText();
        String id = entity.id().getText();

        Class<?> entityClass = dictionary.getEntityClass(entityName);
        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        String relationshipName = relationship.term().getText();
        EntityProjection relationshipProjection = relationship.accept(this);

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .relationship(relationshipName, relationshipProjection)
                .build();
    }

    private EntityProjection visitTerminalCollection(CoreParser.TermContext collectionName) {
        String entityName = collectionName.getText();

        Class<?> entityClass = dictionary.getEntityClass(entityName);

        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .build();
    }
}
