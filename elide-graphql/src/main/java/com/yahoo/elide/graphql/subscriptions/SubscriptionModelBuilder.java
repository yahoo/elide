/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.graphql.GraphQLConversionUtils;
import com.yahoo.elide.graphql.GraphQLNameUtils;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.google.common.collect.Sets;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
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
 * Builds a GraphQL schema for subscriptions.
 */
@Slf4j
public class SubscriptionModelBuilder {
    private Map<Type<?>, GraphQLObjectType> queryObjectRegistry;
    private final String apiVersion;
    private GraphQLConversionUtils generator;
    private GraphQLNameUtils nameUtils;
    private EntityDictionary entityDictionary;
    private DataFetcher dataFetcher;
    private GraphQLArgument filterArgument;
    private Set<Type<?>> excludedEntities;
    private Set<GraphQLObjectType> objectTypes;

    /**
     * Class constructor, constructs the custom arguments to handle mutations.
     * @param entityDictionary elide entity dictionary
     * @param nonEntityDictionary elide non-entity dictionary
     * @param dataFetcher graphQL data fetcher
     */
    public SubscriptionModelBuilder(EntityDictionary entityDictionary,
                        NonEntityDictionary nonEntityDictionary,
                        DataFetcher dataFetcher, String apiVersion) {
        objectTypes = new HashSet<>();
        this.generator = new GraphQLConversionUtils(entityDictionary, nonEntityDictionary, objectTypes);
        this.entityDictionary = entityDictionary;
        this.nameUtils = new GraphQLNameUtils(entityDictionary);
        this.dataFetcher = dataFetcher;
        this.apiVersion = apiVersion;
        excludedEntities = new HashSet<>();
        queryObjectRegistry = new HashMap<>();

        filterArgument = newArgument()
                .name("filter")
                .type(Scalars.GraphQLString)
                .build();
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

        /* Construct root object */
        GraphQLObjectType.Builder root = newObject().name("Subscription");

        String [] subscriptionTypes = { "Added", "Updated", "Deleted"};

        for (Type<?> clazz : rootClasses) {
            for (String subscriptionType : subscriptionTypes) {
                String subscriptionName = entityDictionary.getJsonAliasFor(clazz) + subscriptionType;
                root.field(newFieldDefinition()
                        .name(subscriptionName)
                        .description(EntityDictionary.getEntityDescription(clazz))
                        .argument(filterArgument)
                        .type(buildQueryObject(clazz)));
            }
        }

        GraphQLObjectType queryRoot = root.build();

        /*
         * Walk the object graph (avoiding cycles) and construct the GraphQL output object types.
         */
        entityDictionary.walkEntityGraph(rootClasses, this::buildQueryObject);

        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

        for (GraphQLObjectType objectType : objectTypes) {
            String objectName = objectType.getName();
            for (GraphQLFieldDefinition fieldDefinition : objectType.getFieldDefinitions()) {
                codeRegistry.dataFetcher(
                        FieldCoordinates.coordinates(objectName, fieldDefinition.getName()),
                        dataFetcher);
            }
        }

        /* Construct the schema */
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .subscription(queryRoot)
                .query(queryRoot)
                .codeRegistry(codeRegistry.build())
                .additionalTypes(Sets.newHashSet(queryObjectRegistry.values()))
                .build();

        return schema;
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

        log.debug("Building subscription object for {}", entityClass.getName());

        GraphQLObjectType.Builder builder = newObject()
                .name(nameUtils.toOutputTypeName(entityClass))
                .description(EntityDictionary.getEntityDescription(entityClass));

        String id = entityDictionary.getIdFieldName(entityClass);

        builder.field(newFieldDefinition()
                .name(id)
                .type(Scalars.GraphQLID));

        for (String attribute : entityDictionary.getAttributes(entityClass)) {
            Type<?> attributeClass = entityDictionary.getType(entityClass, attribute);
            if (excludedEntities.contains(attributeClass)) {
                continue;
            }

            log.debug("Building subscription attribute {} {} with arguments {} for entity {}",
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
                    .arguments(generator.attributeArgumentToQueryObject(entityClass, attribute, dataFetcher))
                    .type((GraphQLOutputType) attributeType)
            );
        }

        for (String relationship : entityDictionary.getElideBoundRelationships(entityClass)) {
            log.debug("Resolving relationship {} for {}", relationship, entityClass.getName());
            Type<?> relationshipClass = entityDictionary.getParameterizedType(entityClass, relationship);
            if (excludedEntities.contains(relationshipClass)) {
                continue;
            }

            RelationshipType type = entityDictionary.getRelationshipType(entityClass, relationship);
            String relationshipEntityName = nameUtils.toOutputTypeName(relationshipClass);

            if (type.isToOne()) {
                builder.field(newFieldDefinition()
                        .name(relationship)
                        .type(new GraphQLTypeReference(relationshipEntityName))
                        .build());
            } else {
                builder.field(newFieldDefinition()
                        .name(relationship)
                        .type(new GraphQLList(new GraphQLTypeReference(relationshipEntityName)))
                        .build());
            }
        }

        GraphQLObjectType queryObject = builder.build();
        queryObjectRegistry.put(entityClass, queryObject);
        return queryObject;
    }
}
