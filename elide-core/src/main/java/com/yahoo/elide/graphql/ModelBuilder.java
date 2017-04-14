/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLObjectType.newObject;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;

import graphql.Scalars;
import graphql.java.generator.BuildContext;
import graphql.java.generator.DefaultBuildContext;
import graphql.schema.*;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Constructs a GraphQL schema (query and mutation documents) from an Elide EntityDictionary.
 */
public class ModelBuilder {
    private EntityDictionary dictionary;
    private DataFetcher dataFetcher;
    private GraphQLArgument relationshipOpArg;
    private GraphQLArgument idArgument;
    private GraphQLArgument filterArgument;
    private GraphQLArgument pageOffsetArgument;
    private GraphQLArgument pageFirstArgument;
    private GraphQLArgument sortArgument;
    private GraphQLObjectType metaObject;
    private BuildContext buildContext;

    private Map<Class<?>, MutableGraphQLInputObjectType> inputObjectRegistry;
    private Map<Class<?>, GraphQLObjectType> queryObjectRegistry;

    ModelBuilder(EntityDictionary dictionary, DataFetcher dataFetcher) {
        this.dictionary = dictionary;
        this.dataFetcher = dataFetcher;
        this.buildContext = DefaultBuildContext.newReflectionContext();

        relationshipOpArg = GraphQLArgument.newArgument()
                .name("op")
                .type(buildContext.getInputType(RelationshipOp.class))
                .defaultValue(RelationshipOp.FETCH)
                .build();

        idArgument = GraphQLArgument.newArgument()
                .name("id")
                .type(Scalars.GraphQLString)
                .build();

        filterArgument = GraphQLArgument.newArgument()
                .name("filter")
                .type(Scalars.GraphQLString)
                .build();

        sortArgument = GraphQLArgument.newArgument()
                .name("sort")
                .type(Scalars.GraphQLString)
                .build();

        pageFirstArgument = GraphQLArgument.newArgument()
                .name("first")
                .type(Scalars.GraphQLString)
                .build();

        pageOffsetArgument = GraphQLArgument.newArgument()
                .name("offset")
                .type(Scalars.GraphQLString)
                .build();

        metaObject = GraphQLObjectType.newObject()
                .name("__metaObject")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("page")
                    .dataFetcher(dataFetcher)
                    .type(GraphQLObjectType.newObject()
                        .name("__pageObject")
                        .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("totalPages")
                            .dataFetcher(dataFetcher)
                            .type(Scalars.GraphQLLong)
                            .build()
                        ).field(GraphQLFieldDefinition.newFieldDefinition()
                            .name("totalRecords")
                            .dataFetcher(dataFetcher)
                            .type(Scalars.GraphQLLong)
                            .build()
                        ).build()
                    )
                ).build();

        inputObjectRegistry = new HashMap<>();
        queryObjectRegistry = new HashMap<>();
    }

    /**
     * Builds a GraphQL schema.
     * @return The built schema.
     */
    public GraphQLSchema build() {
        Set<Class<?>> allClasses = dictionary.getBindings();

        if (allClasses.isEmpty()) {
            throw new IllegalArgumentException("None of the provided classes are exported by Elide");
        }

        Set<Class<?>> rootClasses =  allClasses.stream().filter(dictionary::isRoot).collect(Collectors.toSet());

        /*
         * Walk the object graph (avoiding cycles) and construct the GraphQL input object types.
         */
        walkEntityGraph(rootClasses, this::buildInputObjectStub);
        resolveInputObjectRelationships();

        /* Construct root object */
        GraphQLObjectType.Builder root = newObject().name("root");
        for (Class<?> clazz : rootClasses) {
            String entityName = dictionary.getJsonAliasFor(clazz);
            root.field(newFieldDefinition()
                    .name(entityName)
                    .dataFetcher(dataFetcher)
                    .argument(relationshipOpArg)
                    .argument(idArgument)
                    .argument(filterArgument)
                    .argument(sortArgument)
                    .argument(pageFirstArgument)
                    .argument(pageOffsetArgument)
                    .argument(buildInputObjectArgument(clazz, true))
                    .type(new GraphQLList(buildQueryObject(clazz))));
        }

        /*
         * Walk the object graph (avoiding cycles) and construct the GraphQL output object types.
         */
        walkEntityGraph(rootClasses, this::buildQueryObject);

        /* Construct the schema */
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(root)
                .mutation(root)
                .build(new HashSet<>(CollectionUtils.union(
                        queryObjectRegistry.values(),
                        inputObjectRegistry.values()
                )));

        return schema;
    }

    /**
     * Builds a graphQL output object from an entity class.
     * @param entityClass The class to use to construct the output object.
     * @return The graphQL object
     */
    private GraphQLObjectType buildQueryObject(Class<?> entityClass) {
        if (queryObjectRegistry.containsKey(entityClass)) {
            return queryObjectRegistry.get(entityClass);
        }

        String entityName = dictionary.getJsonAliasFor(entityClass);

        GraphQLObjectType.Builder builder = newObject();
        builder.name(entityName);

        String id = dictionary.getIdFieldName(entityClass);
        builder.field(newFieldDefinition()
                    .name(id)
                    .type(Scalars.GraphQLID));

        builder.field(newFieldDefinition()
                    .name("__meta")
                    .type(metaObject));

        for (String attribute : dictionary.getAttributes(entityClass)) {
            Class<?> attributeClass = dictionary.getType(entityClass, attribute);
            GraphQLType attributeType = buildContext.getOutputType(attributeClass);

            if (attributeType == null) {
                continue;
            }

            builder.field(newFieldDefinition()
                    .name(attribute)
                    .dataFetcher(dataFetcher)
                    .type((GraphQLOutputType) attributeType)
            );
        }

        for (String relationship : dictionary.getRelationships(entityClass)) {
            Class<?> relationshipClass = dictionary.getParameterizedType(entityClass, relationship);

            String relationshipEntityName = dictionary.getJsonAliasFor(relationshipClass);
            RelationshipType type = dictionary.getRelationshipType(entityClass, relationship);

            if (type.isToOne()) {
                builder.field(newFieldDefinition()
                                .name(relationship)
                                .dataFetcher(dataFetcher)
                                .argument(relationshipOpArg)
                                .argument(buildInputObjectArgument(relationshipClass, false))
                                .type(new GraphQLTypeReference(relationshipEntityName))
                );
            } else {
                builder.field(newFieldDefinition()
                                .name(relationship)
                                .dataFetcher(dataFetcher)
                                .argument(relationshipOpArg)
                                .argument(filterArgument)
                                .argument(sortArgument)
                                .argument(pageOffsetArgument)
                                .argument(pageFirstArgument)
                                .argument(idArgument)
                                .argument(buildInputObjectArgument(relationshipClass, true))
                                .type(new GraphQLList(new GraphQLTypeReference(relationshipEntityName)))
                );
            }
        }

        GraphQLObjectType queryObject = builder.build();
        queryObjectRegistry.put(entityClass, queryObject);
        return queryObject;
    }

    /**
     * Wraps a constructed GraphQL Input Object in an argument.
     * @param entityClass - The class to construct the input object from.
     * @param asList Whether or not the argument is a single instance or a list.
     * @return The constructed argument.
     */
    private GraphQLArgument buildInputObjectArgument(Class<?> entityClass, boolean asList) {
        GraphQLInputType argumentType = inputObjectRegistry.get(entityClass);

        if (asList) {
             return GraphQLArgument.newArgument()
                    .name("data")
                    .type(new GraphQLList(argumentType))
                    .build();
        } else {
            return GraphQLArgument.newArgument()
                    .name("data")
                    .type(argumentType)
                    .build();
        }
    }

    /**
     * Constructs a stub of an input objects with no relationships resolved.
     * @param clazz The class to translate into an input object.
     * @return The constructed input object stub.
     */
    private GraphQLInputType buildInputObjectStub(Class<?> clazz) {
        String entityName = dictionary.getJsonAliasFor(clazz);

        MutableGraphQLInputObjectType.Builder builder = MutableGraphQLInputObjectType.newMutableInputObject();
        builder.name(entityName + "Input");

        String id = dictionary.getIdFieldName(clazz);

        builder.field(newInputObjectField()
            .name(id)
            .type(Scalars.GraphQLID));

        for (String attribute : dictionary.getAttributes(clazz)) {
            Class<?> attributeClass = dictionary.getType(clazz, attribute);

            GraphQLInputType attributeType = buildContext.getInputType(attributeClass);

            /* If the attribute is an object, we need to change its name so it doesn't conflict with query objects */
            if (attributeType instanceof GraphQLInputObjectType) {
                MutableGraphQLInputObjectType wrappedType =
                    new MutableGraphQLInputObjectType(
                        attributeType.getName() + "Input",
                            ((GraphQLInputObjectType) attributeType).getDescription(),
                            ((GraphQLInputObjectType) attributeType).getFields()
                        );
                attributeType = wrappedType;

            }

            builder.field(newInputObjectField()
                .name(attribute)
                .type(attributeType)
            );
        }

        MutableGraphQLInputObjectType constructed = builder.build();
        inputObjectRegistry.put(clazz, constructed);
        return constructed;
    }

    /**
     * Constructs relationship links for input objects.
     */
    private void resolveInputObjectRelationships() {
         inputObjectRegistry.forEach((clazz, inputObj) -> {
             for (String relationship : dictionary.getRelationships(clazz)) {
                 Class<?> relationshipClass = dictionary.getParameterizedType(clazz, relationship);
                 RelationshipType type = dictionary.getRelationshipType(clazz, relationship);

                 if (type.isToOne()) {
                     inputObj.setField(relationship, newInputObjectField()
                                     .name(relationship)
                                     .type(inputObjectRegistry.get(relationshipClass))
                                     .build()
                     );
                 } else {
                     inputObj.setField(relationship, newInputObjectField()
                                     .name(relationship)
                                     .type(new GraphQLList(inputObjectRegistry.get(relationshipClass)))
                                     .build()
                     );
                 }
             }
        });
    }

    /**
     * Walks the entity graph and performs a transform function on each element.
     * @param entities The roots of the entity graph.
     * @param transform The function to transform each entity class into a result.
     * @param <T> The result type.
     * @return The collection of results.
     */
    private <T> List<T> walkEntityGraph(Set<Class<?>> entities,  Function<Class<?>, T> transform) {
        ArrayList<T> results = new ArrayList<>();
        Queue<Class<?>> toVisit = new ArrayDeque<>(entities);
        Set<Class<?>> visited = new HashSet<>();
        while (! toVisit.isEmpty()) {
            Class<?> clazz = toVisit.remove();
            results.add(transform.apply(clazz));
            visited.add(clazz);

            for (String relationship : dictionary.getRelationships(clazz)) {
                Class<?> relationshipClass = dictionary.getParameterizedType(clazz, relationship);
                if (!visited.contains(relationshipClass)) {
                    toVisit.add(relationshipClass);
                }
            }
        }
        return results;
    }
}
