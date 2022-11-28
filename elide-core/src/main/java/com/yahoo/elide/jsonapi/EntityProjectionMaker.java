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
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.generated.parsers.CoreBaseVisitor;
import com.yahoo.elide.generated.parsers.CoreParser;
import com.yahoo.elide.jsonapi.parser.JsonApiParser;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
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

import javax.ws.rs.core.MultivaluedMap;

/**
 * Converts a JSON-API request (URL and query parameters) into an EntityProjection.
 */
public class EntityProjectionMaker
        extends CoreBaseVisitor<Function<Type<?>, EntityProjectionMaker.NamedEntityProjection>> {

    /**
     * An entity projection labeled with the class name or relationship name it is associated with.
     */
    @Data
    @Builder
    public static class NamedEntityProjection {
        private String name;
        private EntityProjection projection;
    }

    public static final String INCLUDE = "include";

    private EntityDictionary dictionary;
    private MultivaluedMap<String, String> queryParams;
    private Map<String, Set<String>> sparseFields;
    private RequestScope scope;

    public EntityProjectionMaker(EntityDictionary dictionary, RequestScope scope) {
        this.dictionary = dictionary;
        this.queryParams = scope.getQueryParams();
        sparseFields = RequestScope.parseSparseFields(queryParams);
        this.scope = scope;
    }

    public EntityProjection parsePath(String path) {
        return visit(JsonApiParser.parse(path)).apply(null).projection;
    }

    public EntityProjection parseInclude(Type<?> entityClass) {
        return EntityProjection.builder()
                .type(entityClass)
                .arguments(getDefaultEntityArguments(entityClass))
                .relationships(toRelationshipSet(getIncludedRelationships(entityClass)))
                .build();
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitRootCollectionLoadEntities(
            CoreParser.RootCollectionLoadEntitiesContext ctx) {
        return visitTerminalCollection(ctx.term(), true);
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitSubCollectionReadCollection(
            CoreParser.SubCollectionReadCollectionContext ctx) {
        return visitTerminalCollection(ctx.term(), false);
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitRootCollectionSubCollection(
            CoreParser.RootCollectionSubCollectionContext ctx) {
        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection());
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitSubCollectionSubCollection(
            CoreParser.SubCollectionSubCollectionContext ctx) {
        return visitEntityWithSubCollection(ctx.entity(), ctx.subCollection());
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitRootCollectionRelationship(
            CoreParser.RootCollectionRelationshipContext ctx) {
        return visitEntityWithRelationship(ctx.entity(), ctx.relationship());
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitSubCollectionRelationship(
            CoreParser.SubCollectionRelationshipContext ctx) {
        return visitEntityWithRelationship(ctx.entity(), ctx.relationship());
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitRootCollectionLoadEntity(
            CoreParser.RootCollectionLoadEntityContext ctx) {
        return unused -> ctx.entity().accept(this).apply(null);
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitSubCollectionReadEntity(
            CoreParser.SubCollectionReadEntityContext ctx) {
        return parentClass -> ctx.entity().accept(this).apply(parentClass);
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitRelationship(CoreParser.RelationshipContext ctx) {
        return (parentClass) -> {
            String entityName = ctx.term().getText();

            Type<?> entityClass = getEntityClass(parentClass, entityName);
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
                        .arguments(getDefaultEntityArguments(entityClass))
                        .type(entityClass)
                        .build()
                    ).build();
        };
    }

    @Override
    public Function<Type<?>, NamedEntityProjection> visitEntity(CoreParser.EntityContext ctx) {
        return (parentClass) -> {
            String entityName = ctx.term().getText();

            Type<?> entityClass = getEntityClass(parentClass, entityName);

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .type(entityClass)
                        .arguments(getDefaultEntityArguments(entityClass))
                        .attributes(getSparseAttributes(entityClass))
                        .relationships(toRelationshipSet(getRequiredRelationships(entityClass)))
                        .build()
                    ).build();
        };
    }

    @Override
    protected Function<Type<?>, NamedEntityProjection> aggregateResult(
            Function<Type<?>, NamedEntityProjection> aggregate,
            Function<Type<?>, NamedEntityProjection> nextResult) {

        if (aggregate == null) {
            return nextResult;
        }
        return aggregate;
    }

    public EntityProjection visitIncludePath(Path path) {
        Path.PathElement pathElement = path.getPathElements().get(0);
        int size = path.getPathElements().size();

        Type<?> entityClass = pathElement.getFieldType();

        if (size > 1) {
            Path nextPath = new Path(path.getPathElements().subList(1, size));
            EntityProjection relationshipProjection = visitIncludePath(nextPath);

            return EntityProjection.builder()
                .relationships(toRelationshipSet(getSparseRelationships(entityClass)))
                .relationship(nextPath.getPathElements().get(0).getFieldName(), relationshipProjection)
                .attributes(getSparseAttributes(entityClass))
                .filterExpression(scope.getFilterExpressionByType(entityClass).orElse(null))
                .type(entityClass)
                .arguments(getDefaultEntityArguments(entityClass))
                .build();
        }

        return EntityProjection.builder()
                .relationships(toRelationshipSet(getSparseRelationships(entityClass)))
                .attributes(getSparseAttributes(entityClass))
                .type(entityClass)
                .arguments(getDefaultEntityArguments(entityClass))
                .filterExpression(scope.getFilterExpressionByType(entityClass).orElse(null))
                .build();
    }

    private Function<Type<?>, NamedEntityProjection> visitEntityWithSubCollection(CoreParser.EntityContext entity,
                                                            CoreParser.SubCollectionContext subCollection) {
        return (parentClass) -> {
            String entityName = entity.term().getText();

            Type<?> entityClass = getEntityClass(parentClass, entityName);

            NamedEntityProjection projection = subCollection.accept(this).apply(entityClass);

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .type(entityClass)
                        .arguments(getDefaultEntityArguments(entityClass))
                        .relationship(projection.name, projection.projection)
                        .build()
                    ).build();
        };
    }

    private Function<Type<?>, NamedEntityProjection> visitEntityWithRelationship(CoreParser.EntityContext entity,
                                                         CoreParser.RelationshipContext relationship) {
        return (parentClass) -> {
            String entityName = entity.term().getText();

            Type<?> entityClass = getEntityClass(parentClass, entityName);

            String relationshipName = relationship.term().getText();
            NamedEntityProjection relationshipProjection = relationship.accept(this).apply(entityClass);

            FilterExpression filter = scope.getFilterExpressionByType(entityClass).orElse(null);

            return NamedEntityProjection.builder()
                    .name(entityName)
                    .projection(EntityProjection.builder()
                        .type(entityClass)
                        .arguments(getDefaultEntityArguments(entityClass))
                        .filterExpression(filter)
                        .relationships(toRelationshipSet(getRequiredRelationships(entityClass)))
                        .relationship(relationshipName, relationshipProjection.projection)
                        .build()
                    ).build();
        };
    }

    private Function<Type<?>, NamedEntityProjection> visitTerminalCollection(CoreParser.TermContext collectionName,
                                                                              boolean isRoot) {
        return (parentClass) -> {
            String collectionNameText = collectionName.getText();

            Type<?> entityClass = getEntityClass(parentClass, collectionNameText);

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
                        .arguments(getDefaultEntityArguments(entityClass))
                        .type(entityClass)
                        .build()
                    ).build();
        };
    }

    private Type<?> getEntityClass(Type<?> parentClass, String entityLabel) {

            //entityLabel represents a root collection.
            if (parentClass == null) {

                Type<?> entityClass = dictionary.getEntityClass(entityLabel, scope.getApiVersion());

                if (entityClass != null) {
                    return entityClass;
                }


            //entityLabel represents a relationship.
            } else if (dictionary.isRelation(parentClass, entityLabel)) {
                return dictionary.getParameterizedType(parentClass, entityLabel);
            }

            throw new InvalidCollectionException(entityLabel);
    }

    private Map<String, EntityProjection> getIncludedRelationships(Type<?> entityClass) {
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

    private Set<Argument> getDefaultAttributeArguments(Type<?> entityClass, String attributeName) {
        return dictionary.getAttributeArguments(entityClass, attributeName)
                .stream()
                .map(argumentType -> {
                    return Argument.builder()
                            .name(argumentType.getName())
                            .value(argumentType.getDefaultValue())
                            .build();
                })
                .collect(Collectors.toSet());
    }

    private Set<Argument> getDefaultEntityArguments(Type<?> entityClass) {
        return dictionary.getEntityArguments(entityClass)
                .stream()
                .map(argumentType -> {
                    return Argument.builder()
                            .name(argumentType.getName())
                            .value(argumentType.getDefaultValue())
                            .build();
                })
                .collect(Collectors.toSet());
    }

    private Set<Attribute> getSparseAttributes(Type<?> entityClass) {
        Set<String> allAttributes = new LinkedHashSet<>(dictionary.getAttributes(entityClass));

        Set<String> sparseFieldsForEntity = sparseFields.get(dictionary.getJsonAliasFor(entityClass));
        if (CollectionUtils.isEmpty(sparseFieldsForEntity)) {
            sparseFieldsForEntity = allAttributes;
        } else {
            Set<String> allRelationships = new LinkedHashSet<>(dictionary.getRelationships(entityClass));
            validateSparseFields(sparseFieldsForEntity, allAttributes, allRelationships, entityClass);
            sparseFieldsForEntity = Sets.intersection(allAttributes, sparseFieldsForEntity);
        }

        return sparseFieldsForEntity.stream()
                .map(attributeName -> Attribute.builder()
                    .name(attributeName)
                    .type(dictionary.getType(entityClass, attributeName))
                    .arguments(getDefaultAttributeArguments(entityClass, attributeName))
                    .build())
                .collect(Collectors.toSet());
    }

    private Map<String, EntityProjection> getSparseRelationships(Type<?> entityClass) {
        Set<String> allRelationships = new LinkedHashSet<>(dictionary.getRelationships(entityClass));
        Set<String> sparseFieldsForEntity = sparseFields.get(dictionary.getJsonAliasFor(entityClass));

        if (CollectionUtils.isEmpty(sparseFieldsForEntity)) {
            sparseFieldsForEntity = allRelationships;
        } else {
            Set<String> allAttributes = new LinkedHashSet<>(dictionary.getAttributes(entityClass));
            validateSparseFields(sparseFieldsForEntity, allAttributes, allRelationships, entityClass);
            sparseFieldsForEntity = Sets.intersection(allRelationships, sparseFieldsForEntity);
        }

        return sparseFieldsForEntity.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        (relationshipName) -> {
                            FilterExpression filter = scope.getExpressionForRelation(entityClass, relationshipName)
                                    .orElse(null);

                            return EntityProjection.builder()
                                    .type(dictionary.getParameterizedType(entityClass, relationshipName))
                                    .arguments(getDefaultEntityArguments(entityClass))
                                    .filterExpression(filter)
                                    .build();
                        }
                ));
    }

    private void validateSparseFields(Set<String> sparseFieldsForEntity, Set<String> allAttributes,
                    Set<String> allRelationships, Type<?> entityClass) {
        String unknownSparseFields = sparseFieldsForEntity.stream()
                        .filter(field -> !(allAttributes.contains(field) || allRelationships.contains(field)))
                        .collect(Collectors.joining(", "));

        if (!unknownSparseFields.isEmpty()) {
            throw new InvalidValueException(String.format("%s does not contain the fields: [%s]",
                            dictionary.getJsonAliasFor(entityClass), unknownSparseFields));
        }
    }

    private Map<String, EntityProjection> getRequiredRelationships(Type<?> entityClass) {
        return Stream.concat(
                getIncludedRelationships(entityClass).entrySet().stream(),
                getSparseRelationships(entityClass).entrySet().stream()
        ).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                EntityProjection::merge
        ));
    }

    private Set<Path> getIncludePaths(Type<?> entityClass) {
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
