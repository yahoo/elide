/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidCollectionException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.generated.parsers.CoreBaseVisitor;
import com.yahoo.elide.generated.parsers.CoreParser;
import com.yahoo.elide.jsonapi.parser.JsonApiParser;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Converts a JSON-API request (URL and query parameters) into an EntityProjection.
 */
public class EntityProjectionMaker
        extends CoreBaseVisitor<Function<Class<?>, EntityProjectionMaker.NamedEntityProjection>> {

    /**
     * An entity projection labeled with the class name or relationship name it is associated with.
     */
    @Data
    @Builder
    public static class NamedEntityProjection {
        private String name;
        private EntityProjection projection;
    }

    private static final String INCLUDE = "include";

    private EntityDictionary dictionary;
    private MultivaluedMap<String, String> queryParams;
    private Map<String, Set<String>> sparseFields;
    private RequestScope scope;

    public EntityProjectionMaker(EntityDictionary dictionary, RequestScope scope) {
        this.dictionary = dictionary;
        this.queryParams = scope.getQueryParams().orElseGet(MultivaluedHashMap::new);
        sparseFields = RequestScope.parseSparseFields(queryParams);
        this.scope = scope;
    }

    public EntityProjection parsePath(String path) {
        return visit(JsonApiParser.parse(path)).apply(null).projection;
    }

    public EntityProjection parseInclude(Class<?> entityClass) {
        return EntityProjection.builder()
                .type(entityClass)
                .relationships(toRelationshipSet(getIncludedRelationships(entityClass)))
                .build();
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitRootCollectionLoadEntities(
            CoreParser.RootCollectionLoadEntitiesContext ctx) {
        return visitTerminalCollection(ctx.term(), true);
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitSubCollectionReadCollection(
            CoreParser.SubCollectionReadCollectionContext ctx) {
        return visitTerminalCollection(ctx.term(), false);
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitRootCollectionSubCollection(
            CoreParser.RootCollectionSubCollectionContext ctx) {
        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitSubCollectionSubCollection(
            CoreParser.SubCollectionSubCollectionContext ctx) {
        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitRootCollectionRelationship(
            CoreParser.RootCollectionRelationshipContext ctx) {
        return visitEntityWithRelationship(ctx.entity(), ctx.relationship());
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitSubCollectionRelationship(
            CoreParser.SubCollectionRelationshipContext ctx) {
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

            Class<?> entityClass = getEntityClass(parentClass, entityName);
            FilterExpression filter = scope.getExpressionForRelation(parentClass, entityName).orElse(null);

            Sorting sorting = SortingImpl.parseQueryParams(scope.getQueryParams(), entityClass, dictionary);
            Pagination pagination = PaginationImpl.parseQueryParams(entityClass,
                    scope.getQueryParams(), scope.getElideSettings());

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .filterExpression(filter)
                        .sorting(sorting)
                        .pagination(pagination)
                        .type(entityClass)
                        .build()
                    ).build();
        };
    }

    @Override
    public Function<Class<?>, NamedEntityProjection> visitEntity(CoreParser.EntityContext ctx) {
        return (parentClass) -> {
            String entityName = ctx.term().getText();

            Class<?> entityClass = getEntityClass(parentClass, entityName);

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .type(entityClass)
                        .attributes(getSparseAttributes(entityClass))
                        .relationships(toRelationshipSet(getRequiredRelationships(entityClass)))
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
        } else {
            return aggregate;
        }
    }

    public EntityProjection visitIncludePath(Path path) {
        Path.PathElement pathElement = path.getPathElements().get(0);
        int size = path.getPathElements().size();

        Class<?> entityClass = pathElement.getFieldType();

        if (size > 1) {
            Path nextPath = new Path(path.getPathElements().subList(1, size));
            EntityProjection relationshipProjection = visitIncludePath(nextPath);

            return EntityProjection.builder()
                .relationships(toRelationshipSet(getSparseRelationships(entityClass)))
                .relationship(nextPath.getPathElements().get(0).getFieldName(), relationshipProjection)
                .attributes(getSparseAttributes(entityClass))
                .filterExpression(scope.getFilterExpressionByType(entityClass).orElse(null))
                .type(entityClass)
                .build();
        }

        return EntityProjection.builder()
                .relationships(toRelationshipSet(getSparseRelationships(entityClass)))
                .attributes(getSparseAttributes(entityClass))
                .type(entityClass)
                .filterExpression(scope.getFilterExpressionByType(entityClass).orElse(null))
                .build();
    }

    private Function<Class<?>, NamedEntityProjection> visitEntityWithSubCollection(CoreParser.EntityContext entity,
                                                            CoreParser.SubCollectionContext subCollection) {
        return (parentClass) -> {
            String entityName = entity.term().getText();

            Class<?> entityClass = getEntityClass(parentClass, entityName);

            NamedEntityProjection projection = subCollection.accept(this).apply(entityClass);

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
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

            Class<?> entityClass = getEntityClass(parentClass, entityName);

            String relationshipName = relationship.term().getText();
            NamedEntityProjection relationshipProjection = relationship.accept(this).apply(entityClass);

            FilterExpression filter = scope.getFilterExpressionByType(entityClass).orElse(null);

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .type(entityClass)
                        .filterExpression(filter)
                        .relationships(toRelationshipSet(getRequiredRelationships(entityClass)))
                        .relationship(relationshipName, relationshipProjection.projection)
                        .build()
                    ).build();
        };
    }

    private Function<Class<?>, NamedEntityProjection> visitTerminalCollection(CoreParser.TermContext collectionName,
                                                                              boolean isRoot) {
        return (parentClass) -> {
            String collectionNameText = collectionName.getText();

            Class<?> entityClass = getEntityClass(parentClass, collectionNameText);

            if (isRoot && !dictionary.isRoot(entityClass)) {
                throw new InvalidCollectionException(collectionNameText);
            }

            FilterExpression filter;
            if (parentClass == null) {
                filter = scope.getLoadFilterExpression(entityClass).orElse(null);
            } else {
                filter = scope.getExpressionForRelation(parentClass, collectionNameText).orElse(null);
            }

            Sorting sorting = SortingImpl.parseQueryParams(scope.getQueryParams(), entityClass, dictionary);
            Pagination pagination = PaginationImpl.parseQueryParams(entityClass,
                    scope.getQueryParams(), scope.getElideSettings());

            return NamedEntityProjection.builder()
                    .name(collectionNameText)
                    .projection(EntityProjection.builder()
                        .filterExpression(filter)
                        .sorting(sorting)
                        .pagination(pagination)
                        .relationships(toRelationshipSet(getRequiredRelationships(entityClass)))
                        .attributes(getSparseAttributes(entityClass))
                        .type(entityClass)
                        .build()
                    ).build();
        };
    }

    private Class<?> getEntityClass(Class<?> parentClass, String entityLabel) {

            //entityLabel represents a root collection.
            if (parentClass == null) {

                Class<?> entityClass = dictionary.getEntityClass(entityLabel, scope.getApiVersion());

                if (entityClass != null) {
                    return entityClass;
                }


            //entityLabel represents a relationship.
            } else if (dictionary.isRelation(parentClass, entityLabel)) {
                return dictionary.getParameterizedType(parentClass, entityLabel);
            }

            throw new InvalidCollectionException(entityLabel);
    }

    private Map<String, EntityProjection> getIncludedRelationships(Class<?> entityClass) {
        Set<Path> includePaths = getIncludePaths(entityClass);

        Map<String, EntityProjection> relationships = includePaths.stream()
                .map((path) -> Pair.of(path.getPathElements().get(0).getFieldName(), visitIncludePath(path)))
                .collect(Collectors.toMap(
                        Pair::getKey,
                        Pair::getValue,
                        EntityProjection::merge
                ));

        return relationships;
    }

    private Set<Attribute> getSparseAttributes(Class<?> entityClass) {
        Set<String> allAttributes = new LinkedHashSet<>(dictionary.getAttributes(entityClass));

        Set<String> sparseFieldsForEntity = sparseFields.get(dictionary.getJsonAliasFor(entityClass));
        if (sparseFieldsForEntity == null || sparseFieldsForEntity.isEmpty()) {
            sparseFieldsForEntity = allAttributes;
        }

        return Sets.intersection(allAttributes, sparseFieldsForEntity).stream()
                .map(attributeName -> Attribute.builder()
                    .name(attributeName)
                    .type(dictionary.getType(entityClass, attributeName))
                    .build())
                .collect(Collectors.toSet());
    }

    private Map<String, EntityProjection> getSparseRelationships(Class<?> entityClass) {
        Set<String> allRelationships = new LinkedHashSet<>(dictionary.getRelationships(entityClass));
        Set<String> sparseFieldsForEntity = sparseFields.get(dictionary.getJsonAliasFor(entityClass));

        if (sparseFieldsForEntity == null || sparseFieldsForEntity.isEmpty()) {
            sparseFieldsForEntity = allRelationships;
        }

        sparseFieldsForEntity = Sets.intersection(allRelationships, sparseFieldsForEntity);

        return sparseFieldsForEntity.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        (relationshipName) -> {
                            FilterExpression filter = scope.getExpressionForRelation(entityClass, relationshipName)
                                    .orElse(null);

                            return EntityProjection.builder()
                                    .type(dictionary.getParameterizedType(entityClass, relationshipName))
                                    .filterExpression(filter)
                                    .build();
                        }
                ));
    }

    private Map<String, EntityProjection> getRequiredRelationships(Class<?> entityClass) {
        return Stream.concat(
                getIncludedRelationships(entityClass).entrySet().stream(),
                getSparseRelationships(entityClass).entrySet().stream()
        ).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                EntityProjection::merge
        ));
    }

    private Set<Path> getIncludePaths(Class<?> entityClass) {
        if (queryParams.get(INCLUDE) != null) {
            return queryParams.get(INCLUDE).stream()
                    .flatMap(param -> Arrays.stream(param.split(",")))
                    .map(pathString -> new Path(entityClass, dictionary, pathString))
                    .collect(Collectors.toSet());
        }

        return new LinkedHashSet<>();
    }

    private Set<Relationship> toRelationshipSet(Map<String, EntityProjection> relationships) {
        return relationships.entrySet().stream()
                .map(entry -> Relationship.builder()
                        .name(entry.getKey())
                        .alias(entry.getKey())
                        .projection(entry.getValue())
                        .build())
                .collect(Collectors.toSet());
    }
}
