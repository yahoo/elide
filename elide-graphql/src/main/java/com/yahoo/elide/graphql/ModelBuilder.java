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
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;

import org.apache.commons.collections4.CollectionUtils;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
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
    public static final String ARGUMENT_IDS = "ids";
    public static final String ARGUMENT_FILTER = "filter";
    public static final String ARGUMENT_SORT = "sort";
    public static final String ARGUMENT_FIRST = "first";
    public static final String ARGUMENT_AFTER = "after";
    public static final String ARGUMENT_OPERATION = "op";
    public static final String OBJECT_PAGE_INFO = "PageInfo";
    public static final String OBJECT_MUTATION = "Mutation";
    public static final String OBJECT_QUERY = "Query";

    private EntityDictionary entityDictionary;
    private DataFetcher dataFetcher;
    private GraphQLArgument relationshipOpArg;
    private GraphQLArgument idArgument;
    private GraphQLArgument filterArgument;
    private GraphQLArgument pageOffsetArgument;
    private GraphQLArgument pageFirstArgument;
    private GraphQLArgument sortArgument;
    private GraphQLConversionUtils generator;
    private GraphQLNameUtils nameUtils;
    private GraphQLObjectType pageInfoObject;
    private final String apiVersion;

    private Map<Type<?>, MutableGraphQLInputObjectType> inputObjectRegistry;
    private Map<Type<?>, GraphQLObjectType> queryObjectRegistry;
    private Map<Type<?>, GraphQLObjectType> connectionObjectRegistry;
    private Set<Type<?>> excludedEntities;

    /**
     * Class constructor, constructs the custom arguments to handle mutations.
     * @param entityDictionary elide entity dictionary
     * @param nonEntityDictionary elide non-entity dictionary
     * @param dataFetcher graphQL data fetcher
     */
    public ModelBuilder(EntityDictionary entityDictionary,
                        NonEntityDictionary nonEntityDictionary,
                        DataFetcher dataFetcher, String apiVersion) {
        this.generator = new GraphQLConversionUtils(entityDictionary, nonEntityDictionary);
        this.entityDictionary = entityDictionary;
        this.nameUtils = new GraphQLNameUtils(entityDictionary);
        this.dataFetcher = dataFetcher;
        this.apiVersion = apiVersion;

        relationshipOpArg = newArgument()
                .name(ARGUMENT_OPERATION)
                .type(generator.classToEnumType(ClassType.of(RelationshipOp.class)))
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
                .name(OBJECT_PAGE_INFO)
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

    public void withExcludedEntities(Set<Type<?>> excludedEntities) {
        this.excludedEntities = excludedEntities;
    }

    /**
     * Builds a GraphQL schema.
     * @return The built schema.
     */
    public GraphQLSchema build() {
        Set<Type<?>> allClasses = entityDictionary.getBoundClassesByVersion(apiVersion);

        if (allClasses.isEmpty()) {
            throw new IllegalArgumentException("None of the provided classes are exported by Elide");
        }

        Set<Type<?>> rootClasses =  allClasses.stream().filter(entityDictionary::isRoot).collect(Collectors.toSet());

        /*
         * Walk the object graph (avoiding cycles) and construct the GraphQL input object types.
         */
        entityDictionary.walkEntityGraph(rootClasses, this::buildInputObjectStub);
        resolveInputObjectRelationships();

        /* Construct root object */
        GraphQLObjectType.Builder root = newObject().name(OBJECT_QUERY);
        for (Type<?> clazz : rootClasses) {
            String entityName = entityDictionary.getJsonAliasFor(clazz);
            root.field(newFieldDefinition()
                    .name(entityName)
                    .description(EntityDictionary.getEntityDescription(clazz))
                    .dataFetcher(dataFetcher)
                    .argument(relationshipOpArg)
                    .argument(idArgument)
                    .argument(filterArgument)
                    .argument(sortArgument)
                    .argument(pageFirstArgument)
                    .argument(pageOffsetArgument)
                    .argument(buildInputObjectArgument(clazz, true))
                    .argument(generator.entityArgumentToQueryObject(clazz, entityDictionary))
                    .type(buildConnectionObject(clazz)));
        }

        GraphQLObjectType queryRoot = root.build();
        GraphQLObjectType mutationRoot = root.name(OBJECT_MUTATION).build();

        /*
         * Walk the object graph (avoiding cycles) and construct the GraphQL output object types.
         */
        entityDictionary.walkEntityGraph(rootClasses, this::buildConnectionObject);

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
    private GraphQLObjectType buildConnectionObject(Type<?> entityClass) {
        if (connectionObjectRegistry.containsKey(entityClass)) {
            return connectionObjectRegistry.get(entityClass);
        }

        String entityName = nameUtils.toConnectionName(entityClass);

        GraphQLObjectType connectionObject = newObject()
                .name(entityName)
                .field(newFieldDefinition()
                        .name("edges")
                        .dataFetcher(dataFetcher)
                        .type(buildEdgesObject(entityClass, buildQueryObject(entityClass))))
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
    private GraphQLObjectType buildQueryObject(Type<?> entityClass) {
        if (queryObjectRegistry.containsKey(entityClass)) {
            return queryObjectRegistry.get(entityClass);
        }

        log.debug("Building query object for {}", entityClass.getName());

        GraphQLObjectType.Builder builder = newObject()
                .name(nameUtils.toNodeName(entityClass))
                .description(EntityDictionary.getEntityDescription(entityClass));

        String id = entityDictionary.getIdFieldName(entityClass);

        /* our id types are DeferredId objects (not Scalars.GraphQLID) */
        builder.field(newFieldDefinition()
                .name(id)
                .dataFetcher(dataFetcher)
                .type(GraphQLScalars.GRAPHQL_DEFERRED_ID));

        for (String attribute : entityDictionary.getAttributes(entityClass)) {
            Type<?> attributeClass = entityDictionary.getType(entityClass, attribute);
            if (excludedEntities.contains(attributeClass)) {
                continue;
            }

            log.debug("Building query attribute {} {} with arguments {} for entity {}",
                    attribute,
                    attributeClass.getName(),
                    entityDictionary.getAttributeArguments(attributeClass, attribute).toString(),
                    entityClass.getName());

            GraphQLType attributeType =
                    generator.attributeToQueryObject(entityClass, attributeClass, attribute, dataFetcher);

            if (attributeType == null) {
                continue;
            }

            builder.field(newFieldDefinition()
                    .name(attribute)
                    .argument(generator.attributeArgumentToQueryObject(entityClass, attribute, dataFetcher))
                    .dataFetcher(dataFetcher)
                    .type((GraphQLOutputType) attributeType)
            );
        }

        for (String relationship : entityDictionary.getElideBoundRelationships(entityClass)) {
            Type<?> relationshipClass = entityDictionary.getParameterizedType(entityClass, relationship);
            if (excludedEntities.contains(relationshipClass)) {
                continue;
            }

            String relationshipEntityName = nameUtils.toConnectionName(relationshipClass);
            RelationshipType type = entityDictionary.getRelationshipType(entityClass, relationship);

            if (type.isToOne()) {
                builder.field(newFieldDefinition()
                                .name(relationship)
                                .dataFetcher(dataFetcher)
                                .argument(relationshipOpArg)
                                .argument(buildInputObjectArgument(relationshipClass, false))
                                .argument(generator.entityArgumentToQueryObject(relationshipClass, entityDictionary))
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
                                .argument(generator.entityArgumentToQueryObject(relationshipClass, entityDictionary))
                                .type(new GraphQLTypeReference(relationshipEntityName))
                );
            }
        }

        GraphQLObjectType queryObject = builder.build();
        queryObjectRegistry.put(entityClass, queryObject);
        return queryObject;
    }

    private GraphQLList buildEdgesObject(Type<?> relationClass, GraphQLOutputType entityType) {
        return new GraphQLList(newObject()
                .name(nameUtils.toEdgesName(relationClass))
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
    private GraphQLArgument buildInputObjectArgument(Type<?> entityClass, boolean asList) {
        GraphQLInputType argumentType = inputObjectRegistry.get(entityClass);

        if (asList) {
            return newArgument()
                .name(ARGUMENT_DATA)
                .type(new GraphQLList(argumentType))
                .build();
        }
        return newArgument()
            .name(ARGUMENT_DATA)
            .type(argumentType)
            .build();
    }

    /**
     * Constructs a stub of an input objects with no relationships resolved.
     * @param clazz The class to translate into an input object.
     * @return The constructed input object stub.
     */
    private GraphQLInputType buildInputObjectStub(Type<?> clazz) {
        log.debug("Building input object for {}", clazz.getName());

        MutableGraphQLInputObjectType.Builder builder = MutableGraphQLInputObjectType.newMutableInputObject();
        builder.name(nameUtils.toInputTypeName(clazz));

        String id = entityDictionary.getIdFieldName(clazz);
        if (id != null) {
            builder.field(newInputObjectField()
                    .name(id)
                    .type(Scalars.GraphQLID));
        }

        for (String attribute : entityDictionary.getAttributes(clazz)) {
            Type<?> attributeClass = entityDictionary.getType(clazz, attribute);

            if (excludedEntities.contains(attributeClass)) {
                continue;
            }

            log.debug("Building input attribute {} {} for entity {}",
                    attribute,
                    attributeClass.getName(),
                    clazz.getName());

            GraphQLInputType attributeType = generator.attributeToInputObject(clazz, attributeClass, attribute);

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
            for (String relationship : entityDictionary.getElideBoundRelationships(clazz)) {
                log.debug("Resolving relationship {} for {}", relationship, clazz.getName());
                Type<?> relationshipClass = entityDictionary.getParameterizedType(clazz, relationship);
                if (excludedEntities.contains(relationshipClass)) {
                    continue;
                }

                RelationshipType type = entityDictionary.getRelationshipType(clazz, relationship);

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
