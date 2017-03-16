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
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Constructs a GraphQL schema (query and mutation documents) from an Elide EntityDictionary.
 */
public class ModelBuilder {
    private EntityDictionary dictionary;
    private DataFetcher dataFetcher;
    private GraphQLArgument relationshipOpArg;
    private GraphQLArgument idArgument;
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

        GraphQLObjectType.Builder root = newObject().name("root");
        for (Class<?> clazz : rootClasses) {
            String entityName = dictionary.getJsonAliasFor(clazz);
            root.field(newFieldDefinition()
                    .name(entityName)
                    .dataFetcher(dataFetcher)
                    .argument(relationshipOpArg)
                    .argument(idArgument)
                    .argument(buildInputObjectArgument(clazz, true))
                    .type(new GraphQLList(buildQueryObject(clazz))));
        }


        /*
         * Search the object graph, avoid cycles, and construct the GraphQL output object types.
         */
        Queue<Class<?>> toVisit = new ArrayDeque<>(rootClasses);
        while (! toVisit.isEmpty()) {
            Class<?> clazz = toVisit.remove();
            buildQueryObject(clazz);

            for (String relationship : dictionary.getRelationships(clazz)) {
                Class<?> relationshipClass = dictionary.getParameterizedType(clazz, relationship);
                if (!queryObjectRegistry.containsKey(relationshipClass)) {
                    toVisit.add(relationshipClass);
                }
            }
        }

        Set<GraphQLType> allTypes = new HashSet<>(inputObjectRegistry.values());
        allTypes.addAll(queryObjectRegistry.values());

        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(root)
                .mutation(root)
                .build(allTypes);

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
        GraphQLInputType argumentType = buildInputObject(entityClass);

        if (asList) {
             return GraphQLArgument.newArgument()
                    .name("relationship")
                    .type(new GraphQLList(argumentType))
                    .build();
        } else {
            return GraphQLArgument.newArgument()
                    .name("relationship")
                    .type(argumentType)
                    .build();
        }
    }

    /**
     * Builds a nested GraphQL input object that traverses relationships but avoids cycles.
     * @param entityClass The starting entity type to build the input object from.
     * @return Constructed graphql input object type.
     */
    private GraphQLInputType buildInputObject(Class<?> entityClass) {
        if (inputObjectRegistry.containsKey(entityClass)) {
            return inputObjectRegistry.get(entityClass);
        }

        Map<Class<?>, MutableGraphQLInputObjectType> constructing = new HashMap<>();
        Queue<Class<?>> toVisit = new ArrayDeque<>();
        toVisit.add(entityClass);

        /* First pass, construct all of the builders in the object graph without constructing relationships */
        while (!toVisit.isEmpty()) {
            Class<?> clazz = toVisit.remove();

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
                if (attributeType instanceof GraphQLInputObjectType) {
                    MutableGraphQLInputObjectType wrappedType =
                            new MutableGraphQLInputObjectType(
                                    attributeType.getName() + "Input",
                                    ((GraphQLInputObjectType) attributeType).getDescription(),
                                    ((GraphQLInputObjectType) attributeType).getFields()
                                    );
                    attributeType = wrappedType;

                }


                if (attributeType == null) {
                    continue;
                }

                builder.field(newInputObjectField()
                                .name(attribute)
                                .type(attributeType)
                );
            }

            constructing.put(clazz, builder.build());

            for (String relationship : dictionary.getRelationships(clazz)) {
                Class<?> relationshipClass = dictionary.getParameterizedType(clazz, relationship);

                if (!constructing.containsKey(relationshipClass)) {
                    toVisit.add(relationshipClass);
                }
            }
        }

        /* Assign relationships to the partially constructed input objects */
        constructing.forEach((clazz, inputObj) -> {
             for (String relationship : dictionary.getRelationships(clazz)) {
                 Class<?> relationshipClass = dictionary.getParameterizedType(clazz, relationship);
                 RelationshipType type = dictionary.getRelationshipType(clazz, relationship);

                 if (type.isToOne()) {
                     inputObj.setField(relationship, newInputObjectField()
                                     .name(relationship)
                                     .type(constructing.getOrDefault(relationshipClass,
                                             inputObjectRegistry.get(relationshipClass)))
                                     .build()
                     );
                 } else {
                     inputObj.setField(relationship, newInputObjectField()
                                     .name(relationship)
                                     .type(new GraphQLList(constructing.getOrDefault(relationshipClass,
                                             inputObjectRegistry.get(relationshipClass))))
                                     .build()
                     );
                 }
             }
        });

        MutableGraphQLInputObjectType constructed = constructing.get(entityClass);
        inputObjectRegistry.put(entityClass, constructed);
        return constructed;
    }
}
