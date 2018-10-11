/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLObjectType.newObject;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;

import org.apache.commons.collections.CollectionUtils;

import graphql.Scalars;
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
import lombok.extern.slf4j.Slf4j;

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
    public static final String ARGUMENT_DATA = "data";
    public static final String ARGUMENT_INPUT = "Input";
    public static final String ARGUMENT_IDS = "ids";
    public static final String ARGUMENT_FILTER = "filter";
    public static final String ARGUMENT_SORT = "sort";
    public static final String ARGUMENT_FIRST = "first";
    public static final String ARGUMENT_AFTER = "after";
    public static final String ARGUMENT_OPERATION = "op";

    private EntityDictionary dictionary;
    private DataFetcher dataFetcher;
    private GraphQLArgument relationshipOpArg;
    private GraphQLArgument idArgument;
    private GraphQLArgument filterArgument;
    private GraphQLArgument pageOffsetArgument;
    private GraphQLArgument pageFirstArgument;
    private GraphQLArgument sortArgument;
    private GraphQLConversionUtils generator;
    private GraphQLObjectType pageInfoObject;

    private Map<Class<?>, MutableGraphQLInputObjectType> inputObjectRegistry;
    private Map<Class<?>, GraphQLObjectType> queryObjectRegistry;
    private Map<Class<?>, GraphQLObjectType> connectionObjectRegistry;
    private Set<Class<?>> excludedEntities;

    private HashMap<String, GraphQLInputType> convertedInputs = new HashMap<>();

    /**
     * Class constructor, constructs the custom arguments to handle mutations
     * @param dictionary elide entity dictionary
     * @param dataFetcher graphQL data fetcher
     */
    public ModelBuilder(EntityDictionary dictionary, DataFetcher dataFetcher) {
        this.generator = new GraphQLConversionUtils(dictionary);
        this.dictionary = dictionary;
        this.dataFetcher = dataFetcher;

        relationshipOpArg = newArgument()
                .name(ARGUMENT_OPERATION)
                .type(generator.classToEnumType(RelationshipOp.class))
                .defaultValue(RelationshipOp.FETCH)
                .build();

        idArgument = newArgument()
                .name(ARGUMENT_IDS)
                .type(new GraphQLList(Scalars.GraphQLString))
                .build();

        filterArgument = newArgument()
                .name(ARGUMENT_FILTER)
                .type(Scalars.GraphQLString)
                .build();

        sortArgument = newArgument()
                .name(ARGUMENT_SORT)
                .type(Scalars.GraphQLString)
                .build();

        pageFirstArgument = newArgument()
                .name(ARGUMENT_FIRST)
                .type(Scalars.GraphQLString)
                .build();

        pageOffsetArgument = newArgument()
                .name(ARGUMENT_AFTER)
                .type(Scalars.GraphQLString)
                .build();

        pageInfoObject = newObject()
                .name("__pageInfoObject")
                .field(newFieldDefinition()
                        .name("hasNextPage")
                        .dataFetcher(dataFetcher)
                        .type(Scalars.GraphQLBoolean))
                .field(newFieldDefinition()
                        .name("startCursor")
                        .dataFetcher(dataFetcher)
                        .type(Scalars.GraphQLString))
                .field(newFieldDefinition()
                        .name("endCursor")
                        .dataFetcher(dataFetcher)
                        .type(Scalars.GraphQLString))
                .field(newFieldDefinition()
                        .name("totalRecords")
                        .dataFetcher(dataFetcher)
                        .type(Scalars.GraphQLLong))
                .build();

        inputObjectRegistry = new HashMap<>();
        queryObjectRegistry = new HashMap<>();
        connectionObjectRegistry = new HashMap<>();
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
        GraphQLObjectType.Builder root = newObject().name("__root");
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
                    .type(buildConnectionObject(clazz)));
        }

        GraphQLObjectType queryRoot = root.build();
        GraphQLObjectType mutationRoot = root.name("__mutation_root").build();

        /*
         * Walk the object graph (avoiding cycles) and construct the GraphQL output object types.
         */
        dictionary.walkEntityGraph(rootClasses, this::buildConnectionObject);

        /* Construct the schema */
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryRoot)
                .mutation(mutationRoot)
                .build(new HashSet<>(CollectionUtils.union(
                        connectionObjectRegistry.values(),
                        inputObjectRegistry.values()
                )));

        return schema;
    }

    /**
     * Builds a GraphQL connection object from an entity class.
     *
     * @param entityClass The class to use to construct the output object
     * @return The GraphQL object.
     */
    private GraphQLObjectType buildConnectionObject(Class<?> entityClass) {
        if (connectionObjectRegistry.containsKey(entityClass)) {
            return connectionObjectRegistry.get(entityClass);
        }

        String entityName = dictionary.getJsonAliasFor(entityClass);

        GraphQLObjectType connectionObject = newObject()
                .name(entityName)
                .field(newFieldDefinition()
                        .name("edges")
                        .dataFetcher(dataFetcher)
                        .type(buildEdgesObject(entityName, buildQueryObject(entityClass))))
                .field(newFieldDefinition()
                        .name("pageInfo")
                        .dataFetcher(dataFetcher)
                        .type(pageInfoObject))
                .build();

        connectionObjectRegistry.put(entityClass, connectionObject);

        return connectionObject;
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

        GraphQLObjectType.Builder builder = newObject()
                .name("__node__" + entityName);

        String id = dictionary.getIdFieldName(entityClass);

        /* our id types are DeferredId objects (not Scalars.GraphQLID) */
        builder.field(newFieldDefinition()
                .name(id)
                .dataFetcher(dataFetcher)
                .type(GraphQLScalars.GRAPHQL_DEFERRED_ID));

        for (String attribute : dictionary.getAttributes(entityClass)) {
            Class<?> attributeClass = dictionary.getType(entityClass, attribute);
            if (excludedEntities.contains(attributeClass)) {
                continue;
            }

            log.debug("Building query attribute {} {} for entity {}",
                    attribute,
                    attributeClass.getName(),
                    entityClass.getName());

            GraphQLType attributeType =
                    generator.attributeToQueryObject(entityClass, attributeClass, attribute, dataFetcher);

            if (attributeType == null) {
                continue;
            }

            builder.field(newFieldDefinition()
                    .name(attribute)
                    .dataFetcher(dataFetcher)
                    .type((GraphQLOutputType) attributeType)
            );
        }

        for (String relationship : dictionary.getElideBoundRelationships(entityClass)) {
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
                                .type(new GraphQLTypeReference(relationshipEntityName))
                );
            }
        }

        GraphQLObjectType queryObject = builder.build();
        queryObjectRegistry.put(entityClass, queryObject);
        return queryObject;
    }

    private GraphQLList buildEdgesObject(String relationName, GraphQLOutputType entityType) {
        return new GraphQLList(newObject()
                .name("__edges__" + relationName)
                .field(newFieldDefinition()
                        .name("node")
                        .dataFetcher(dataFetcher)
                        .type(entityType))
                .build());
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
            return newArgument()
                .name(ARGUMENT_DATA)
                .type(new GraphQLList(argumentType))
                .build();
        } else {
            return newArgument()
                .name(ARGUMENT_DATA)
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
        log.debug("Building input object for {}", clazz.getName());

        String entityName = dictionary.getJsonAliasFor(clazz);

        MutableGraphQLInputObjectType.Builder builder = MutableGraphQLInputObjectType.newMutableInputObject();
        builder.name(entityName + ARGUMENT_INPUT);

        String id = dictionary.getIdFieldName(clazz);
        builder.field(newInputObjectField()
                .name(id)
                .type(Scalars.GraphQLID));

        for (String attribute : dictionary.getAttributes(clazz)) {
            Class<?> attributeClass = dictionary.getType(clazz, attribute);

            if (excludedEntities.contains(attributeClass)) {
                continue;
            }

            log.debug("Building input attribute {} {} for entity {}",
                    attribute,
                    attributeClass.getName(),
                    clazz.getName());

            GraphQLInputType attributeType = generator.attributeToInputObject(clazz, attributeClass, attribute);

            /* If the attribute is an object, we need to change its name so it doesn't conflict with query objects */
            if (attributeType instanceof GraphQLInputObjectType) {
                String objectName = attributeType.getName() + ARGUMENT_INPUT;
                if (!convertedInputs.containsKey(objectName)) {
                    MutableGraphQLInputObjectType wrappedType =
                            new MutableGraphQLInputObjectType(
                                    objectName,
                                    ((GraphQLInputObjectType) attributeType).getDescription(),
                                    ((GraphQLInputObjectType) attributeType).getFields()
                            );
                    convertedInputs.put(objectName, wrappedType);
                    attributeType = wrappedType;
                } else {
                    attributeType = convertedInputs.get(objectName);
                }
            } else {
                String attributeTypeName = attributeType.getName();
                convertedInputs.putIfAbsent(attributeTypeName, attributeType);
                attributeType = convertedInputs.get(attributeTypeName);
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
            for (String relationship : dictionary.getElideBoundRelationships(clazz)) {
                log.debug("Resolving relationship {} for {}", relationship, clazz.getName());
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
