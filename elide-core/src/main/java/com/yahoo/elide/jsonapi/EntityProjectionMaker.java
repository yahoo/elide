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
import com.yahoo.elide.parsers.JsonApiParser;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Converts a JSON-API request (URL and query parameters) into an EntityProjection.
 * TODO - Parse filter parameters and add them to the projection.
 * TODO - Parse sparse fields and limit the attributes in the projection.
 */
public class EntityProjectionMaker extends CoreBaseVisitor<Function<Class<?>, EntityProjectionMaker.NamedEntityProjection>> {

    @Data
    @Builder
    public static class NamedEntityProjection {
        private String name;
        private EntityProjection projection;
    }

    private static final String INCLUDE = "include";

    private EntityDictionary dictionary;
    private MultivaluedMap<String, String> queryParams;

    public EntityProjectionMaker(EntityDictionary dictionary, MultivaluedMap<String, String> queryParams) {
       this.dictionary = dictionary;
       this.queryParams = queryParams;
    }

    public EntityProjection make(String path) {
        return visit(JsonApiParser.parse(path)).apply(null).projection;
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitRootCollectionLoadEntities(CoreParser.RootCollectionLoadEntitiesContext ctx) {
        return visitTerminalCollection(ctx.term());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitSubCollectionReadCollection(CoreParser.SubCollectionReadCollectionContext ctx) {
        return visitTerminalCollection(ctx.term());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitRootCollectionSubCollection(CoreParser.RootCollectionSubCollectionContext ctx) {
        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitSubCollectionSubCollection(CoreParser.SubCollectionSubCollectionContext ctx) {
        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitRootCollectionRelationship(CoreParser.RootCollectionRelationshipContext ctx) {
        return visitEntityWithRelationship(ctx.entity(), ctx.relationship());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitSubCollectionRelationship(CoreParser.SubCollectionRelationshipContext ctx) {
        return visitEntityWithRelationship(ctx.entity(), ctx.relationship());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitRootCollectionLoadEntity(
            CoreParser.RootCollectionLoadEntityContext ctx) {
        return (unused) -> {
            return ctx.entity().accept(this).apply(null);
        };
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitSubCollectionReadEntity(
            CoreParser.SubCollectionReadEntityContext ctx) {
        return (parentClass) -> {
            return ctx.entity().accept(this).apply(parentClass);
        };
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitRelationship(CoreParser.RelationshipContext ctx) {
        return (parentClass) -> {
            String entityName = ctx.term().getText();

            Class<?> entityClass;
            if (parentClass == null) {
                entityClass = dictionary.getEntityClass(entityName);
            } else {
                entityClass = dictionary.getParameterizedType(parentClass, entityName);
            }

            if (entityClass == null) {
                throw new InvalidCollectionException(entityName);
            }

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(entityClass)
                        .build()
                    ).build();
        };
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitEntity(CoreParser.EntityContext ctx) {
        return (parentClass) -> {
            String entityName = ctx.term().getText();
            String id = ctx.id().getText();

            Class<?> entityClass;
            if (parentClass == null) {
                entityClass = dictionary.getEntityClass(entityName);
            } else {
                entityClass = dictionary.getParameterizedType(parentClass, entityName);
            }

            if (entityClass == null) {
                throw new InvalidCollectionException(entityName);
            }

            Set<Attribute> attributes = dictionary.getAttributes(entityClass).stream()
                    .map(attributeName -> Attribute.builder()
                            .name(attributeName)
                            .type(dictionary.getType(entityClass, attributeName))
                            .build())
                    .collect(Collectors.toSet());

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(entityClass)
                        .attributes(attributes)
                        .relationships(getIncludedRelationships(entityClass))
                        .build()
                    ).build();
        };
    }

    @Override
    protected Function<Class<?>, NamedEntityProjection> aggregateResult(
            Function<Class<?>, NamedEntityProjection> aggregate,
            Function<Class<?>, NamedEntityProjection> nextResult) {

        if (aggregate == null) {
            return nextResult;
        } else return aggregate;
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

    private Function<Class<?>, NamedEntityProjection> visitEntityWithSubCollection(CoreParser.EntityContext entity,
                                                            CoreParser.SubCollectionContext subCollection) {
        return (parentClass) -> {
            String entityName = entity.term().getText();
            String id = entity.id().getText();

            Class<?> entityClass;
            if (parentClass == null) {
                entityClass = dictionary.getEntityClass(entityName);
            } else {
                entityClass = dictionary.getParameterizedType(parentClass, entityName);
            }

            if (entityClass == null) {
                throw new InvalidCollectionException(entityName);
            }

            NamedEntityProjection projection = subCollection.accept(this).apply(entityClass);

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(entityClass)
                        .relationship(projection.name, projection.projection)
                        .build()
                    ).build();
        };
    }

    private Function<Class<?>, NamedEntityProjection> visitEntityWithRelationship(CoreParser.EntityContext entity,
                                                         CoreParser.RelationshipContext relationship) {
        return (parentClass) -> {
            String entityName = entity.term().getText();
            String id = entity.id().getText();

            Class<?> entityClass;
            if (parentClass == null) {
                entityClass = dictionary.getEntityClass(entityName);
            } else {
                entityClass = dictionary.getParameterizedType(parentClass, entityName);
            }

            if (entityClass == null || !dictionary.isRoot(entityClass)) {
                throw new InvalidCollectionException(entityName);
            }

            String relationshipName = relationship.term().getText();
            NamedEntityProjection relationshipProjection = relationship.accept(this).apply(entityClass);

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(entityClass)
                        .relationship(relationshipName, relationshipProjection.projection)
                        .build()
                    ).build();
        };
    }

    private Function<Class<?>, NamedEntityProjection> visitTerminalCollection(CoreParser.TermContext collectionName) {
        return (parentClass) -> {
            String collectionNameText = collectionName.getText();

            Class<?> entityClass;
            if (parentClass == null) {
                entityClass = dictionary.getEntityClass(collectionNameText);
            } else {
                entityClass = dictionary.getParameterizedType(parentClass, collectionNameText);
            }

            if (entityClass == null) {
                throw new InvalidCollectionException(collectionNameText);
            }

            Set<Attribute> attributes = dictionary.getAttributes(entityClass).stream()
                    .map(attributeName -> Attribute.builder()
                            .name(attributeName)
                            .type(dictionary.getType(entityClass, attributeName))
                            .build())
                    .collect(Collectors.toSet());

            return NamedEntityProjection.builder()
                    .name(collectionNameText)
                    .projection(EntityProjection.builder()
                        .dictionary(dictionary)
                        .relationships(getIncludedRelationships(entityClass))
                        .attributes(attributes)
                        .type(entityClass)
                        .build()
                    ).build();
        };
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
}
