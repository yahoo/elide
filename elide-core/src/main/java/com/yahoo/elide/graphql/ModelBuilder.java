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
import graphql.schema.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Constructs a GraphQL schema (query and mutation documents) from an Elide EntityDictionary.
 */
@Slf4j
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
    private ObjectGenerator generator;

    private Map<Class<?>, MutableGraphQLInputObjectType> inputObjectRegistry;
    private Map<Class<?>, GraphQLObjectType> queryObjectRegistry;
    private Set<Class<?>> excludedEntities;

    public ModelBuilder(EntityDictionary dictionary, DataFetcher dataFetcher) {
        this.generator = new ObjectGenerator(dictionary);
        this.dictionary = dictionary;
        this.dataFetcher = dataFetcher;

        relationshipOpArg = GraphQLArgument.newArgument()
                .name("op")
                .type(generator.classToEnumType(RelationshipOp.class))
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
        excludedEntities = new HashSet<>();
    }

    public void withExcludedEntities(Set<Class<?>> excludedEntities) {
        this.excludedEntities = excludedEntities;
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
        dictionary.walkEntityGraph(rootClasses, this::buildInputObjectStub);
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
        dictionary.walkEntityGraph(rootClasses, this::buildQueryObject);

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

        log.info("Building query object for {}", entityClass.getName());

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
            if (excludedEntities.contains(attributeClass)) {
                continue;
            }

            log.info("Building query attribute {} {} for entity {}",
                    attribute,
                    attributeClass.getName(),
                    entityClass.getName());

            GraphQLType attributeType = generator.classToQueryType(entityClass, attributeClass, attribute, dataFetcher);

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
            if (excludedEntities.contains(relationshipClass)) {
                continue;
            }

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
        log.info("Building input object for {}", clazz.getName());

        String entityName = dictionary.getJsonAliasFor(clazz);

        MutableGraphQLInputObjectType.Builder builder = MutableGraphQLInputObjectType.newMutableInputObject();
        builder.name(entityName + "Input");

        String id = dictionary.getIdFieldName(clazz);

        builder.field(newInputObjectField()
            .name(id)
            .type(Scalars.GraphQLID));

        for (String attribute : dictionary.getAttributes(clazz)) {
            Class<?> attributeClass = dictionary.getType(clazz, attribute);

            if (excludedEntities.contains(attributeClass)) {
                continue;
            }

            log.info("Building input attribute {} {} for entity {}",
                    attribute,
                    attributeClass.getName(),
                    clazz.getName());

            GraphQLInputType attributeType = generator.classToInputType(clazz, attributeClass, attribute);

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
                 log.info("Resolving relationship {} for {}", relationship, clazz.getName());
                 Class<?> relationshipClass = dictionary.getParameterizedType(clazz, relationship);
                 if (excludedEntities.contains(relationshipClass)) {
                    continue;
                 }

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
}
