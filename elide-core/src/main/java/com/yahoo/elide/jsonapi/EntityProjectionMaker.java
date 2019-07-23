/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidCollectionException;
import com.yahoo.elide.generated.parsers.CoreBaseVisitor;
import com.yahoo.elide.generated.parsers.CoreParser;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Converts a JSON-API request (URL and query parameters) into an EntityProjection.
 * TODO - Parse filter parameters and add them to the projection.
 * TODO - Parse sparse fields and limit the attributes in the projection.
 */
public class EntityProjectionMaker extends CoreBaseVisitor<EntityProjection> {

    private static final String INCLUDE = "include";

    private EntityDictionary dictionary;
    private MultivaluedMap<String, String> queryParams;
    private Class<?> entityClass;


    public EntityProjectionMaker(EntityDictionary dictionary, MultivaluedMap<String, String> queryParams) {
       this.dictionary = dictionary;
       this.queryParams = queryParams;
       entityClass = null;
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
    public EntityProjection visitRootCollectionSubCollection(CoreParser.RootCollectionSubCollectionContext ctx) {
        String relationshipName =
                ((CoreParser.SubCollectionSubCollectionContext) ctx.subCollection()).entity().term().getText();

        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection(), relationshipName);
    }

    @Override
    public EntityProjection visitSubCollectionSubCollection(CoreParser.SubCollectionSubCollectionContext ctx) {
        String relationshipName =
                ((CoreParser.SubCollectionSubCollectionContext) ctx.subCollection()).entity().term().getText();

        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection(), relationshipName);
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
    public EntityProjection visitRootCollectionLoadEntity(CoreParser.RootCollectionLoadEntityContext ctx) {
        return visitTerminalEntity(ctx.entity());
    }

    @Override
    public EntityProjection visitSubCollectionReadEntity(CoreParser.SubCollectionReadEntityContext ctx) {
        return visitTerminalEntity(ctx.entity());
    }

    @Override
    public EntityProjection visitRelationship(CoreParser.RelationshipContext ctx) {
        String entityName = ctx.term().getText();

        Class<?> entityClass = getEntityClass(entityName);
        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .build();
    }

    @Override
    protected EntityProjection aggregateResult(EntityProjection aggregate, EntityProjection nextResult) {
        if (aggregate != null) {
            return aggregate;
        }
        return nextResult;
    }

    private EntityProjection visitTerminalEntity(CoreParser.EntityContext ctx) {
        String entityName = ctx.term().getText();
        String id = ctx.id().getText();

        Class<?> entityClass = getEntityClass(entityName);

        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        Set<Attribute> attributes = dictionary.getAttributes(entityClass).stream()
                .map(attributeName -> Attribute.builder()
                        .name(attributeName)
                        .type(dictionary.getType(entityClass, attributeName))
                        .build())
                .collect(Collectors.toSet());

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .attributes(attributes)
                .relationships(getIncludedRelationships(entityClass))
                .build();
    }



    public EntityProjection visitPath(Path path) {
        Path.PathElement pathElement = path.getPathElements().get(0);
        int size = path.getPathElements().size();

        Class<?> entityClass = pathElement.getFieldType();

        if (size > 1) {
            Path nextPath = new Path(path.getPathElements().subList(1, size - 1));
            EntityProjection relationshipProjection = visitPath(nextPath);

            return EntityProjection.builder()
                .dictionary(dictionary)
                .relationship(pathElement.getFieldName(), relationshipProjection)
                .type(entityClass)
                .build();
        }

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .build();
    }

    private EntityProjection visitEntityWithSubCollection(CoreParser.EntityContext entity,
                                                          CoreParser.SubCollectionContext subCollection,
                                                          String relationshipName) {

        String entityName = entity.term().getText();
        String id = entity.id().getText();

       Class<?> entityClass = getEntityClass(entityName);

        if (entityClass == null) {
            throw new InvalidCollectionException(entityName);
        }

        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(entityClass)
                .relationship(relationshipName, subCollection.accept(this))
                .build();
    }

    private EntityProjection visitEntityWithRelationship(CoreParser.EntityContext entity,
                                                         CoreParser.RelationshipContext relationship) {
        String entityName = entity.term().getText();
        String id = entity.id().getText();

        Class<?> entityClass = getEntityClass(entityName);

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
        String collectionNameText = collectionName.getText();

        Class<?> entityClass = getEntityClass(collectionNameText);

        if (entityClass == null) {
            throw new InvalidCollectionException(collectionNameText);
        }

        Set<Attribute> attributes = dictionary.getAttributes(entityClass).stream()
                .map(attributeName -> Attribute.builder()
                        .name(attributeName)
                        .type(dictionary.getType(entityClass, attributeName))
                        .build())
                .collect(Collectors.toSet());

        return EntityProjection.builder()
            .dictionary(dictionary)
            .relationships(getIncludedRelationships(entityClass))
            .attributes(attributes)
            .type(entityClass)
            .build();
    }

    private Map<String, EntityProjection> getIncludedRelationships(Class<?> entityClass) {
        Set<Path> includePaths = getIncludePaths(entityClass);

        Map<String, EntityProjection> relationships = includePaths.stream()
                .map((path) -> Pair.of(path.getPathElements().get(0).getFieldName(), visitPath(path)))
                .collect(Collectors.toMap(
                        Pair::getKey,
                        Pair::getValue,
                        EntityProjection::mergeRelationships
                ));

        return relationships;
    }

    private Set<Path> getIncludePaths(Class<?> entityClass) {
        if (queryParams.get(INCLUDE) != null) {
            return queryParams.get(INCLUDE).stream()
                    .flatMap(param -> Arrays.stream(param.split(",")))
                    .map(pathString -> new Path(entityClass, dictionary, pathString))
                    .collect(Collectors.toSet());

        }

        return new HashSet<>();
    }

    private Class<?> getEntityClass(String collectionName) {
        if (entityClass == null) {
            entityClass = dictionary.getEntityClass(collectionName);
        } else {
            entityClass = dictionary.getParameterizedType(entityClass, collectionName);
        }
        return entityClass;
    }
}
