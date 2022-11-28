/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.graphql.GraphQLConversionUtils;
import com.yahoo.elide.graphql.GraphQLNameUtils;
import com.yahoo.elide.graphql.GraphQLScalars;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.yahoo.elide.graphql.subscriptions.hooks.TopicType;
import com.google.common.collect.Sets;

import graphql.Scalars;
import graphql.language.EnumTypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLEnumType;
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
    private Map<Type<?>, GraphQLObjectType> queryObjectRegistry;  //Cache of models we've already processed.
    private final String apiVersion;            //apiVersion we are generating a schema for.
    private GraphQLConversionUtils generator;   //Converts attributes (non Elide models) to GraphQL types.
    private GraphQLNameUtils nameUtils;         //Generates type names
    private EntityDictionary entityDictionary;  //Client provided model dictionary
    private DataFetcher dataFetcher;        //Client provided data fetcher
    private GraphQLArgument filterArgument;
    private Set<Type<?>> excludedEntities;  //Client controlled models to skip.
    private Set<Type<?>> relationshipTypes; //Keeps track of which relationship models need to be built.

    public static final String TOPIC_ARGUMENT = "topic";

    /**
     * Class constructor, constructs the custom arguments to handle mutations.
     * @param entityDictionary elide entity dictionary
     * @param nonEntityDictionary elide non-entity dictionary
     * @param dataFetcher graphQL data fetcher
     */
    public SubscriptionModelBuilder(EntityDictionary entityDictionary,
                        NonEntityDictionary nonEntityDictionary,
                        DataFetcher dataFetcher, String apiVersion) {
        this.generator = new GraphQLConversionUtils(entityDictionary, nonEntityDictionary);
        this.entityDictionary = entityDictionary;
        this.nameUtils = new GraphQLNameUtils(entityDictionary);
        this.dataFetcher = dataFetcher;
        this.apiVersion = apiVersion;
        excludedEntities = new HashSet<>();
        queryObjectRegistry = new HashMap<>();
        relationshipTypes = new HashSet<>();

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

        Set<Type<?>> subscriptionClasses =  allClasses.stream()
                .filter((cls) -> entityDictionary.getAnnotation(cls, Subscription.class) != null)
                .collect(Collectors.toSet());

        /* Construct root object */
        GraphQLObjectType.Builder root = newObject().name("Subscription");

        for (Type<?> clazz : subscriptionClasses) {
            Subscription subscription = entityDictionary.getAnnotation(clazz, Subscription.class);
            if (subscription == null) {
                continue;
            }

            GraphQLObjectType subscriptionType = buildQueryObject(clazz);
            String entityName = entityDictionary.getJsonAliasFor(clazz);

            GraphQLFieldDefinition.Builder rootFieldDefinitionBuilder = newFieldDefinition()
                    .name(entityName)
                    .description(EntityDictionary.getEntityDescription(clazz))
                    .argument(filterArgument)
                    .type(subscriptionType);

            if (subscription.operations() != null && subscription.operations().length > 0) {
                GraphQLEnumType.Builder topicTypeBuilder = newEnum().name(nameUtils.toTopicName(clazz));

                for (Subscription.Operation operation : subscription.operations()) {
                    TopicType topicType = TopicType.fromOperation(operation);
                    topicTypeBuilder.value(topicType.name(), topicType);
                }
                topicTypeBuilder.definition(EnumTypeDefinition.newEnumTypeDefinition().build());
                rootFieldDefinitionBuilder.argument(
                        GraphQLArgument.newArgument().name(TOPIC_ARGUMENT).type(topicTypeBuilder.build()).build());
            }

            root.field(rootFieldDefinitionBuilder.build());
        }

        GraphQLObjectType queryRoot = root.build();

        for (Type<?> relationshipType : relationshipTypes) {
            this.buildQueryObject(relationshipType);
        }

        // Attach the client provided dataFetcher to all of the GraphQL subscription objects.
        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

        Set<GraphQLObjectType> objectsThatNeedAFetcher = new HashSet<>(queryObjectRegistry.values());
        objectsThatNeedAFetcher.addAll(generator.getObjectTypes());
        objectsThatNeedAFetcher.add(queryRoot);

        for (GraphQLObjectType objectType : objectsThatNeedAFetcher) {
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
                .type(GraphQLScalars.GRAPHQL_DEFERRED_ID));

        for (String attribute : entityDictionary.getAttributes(entityClass)) {
            Type<?> attributeClass = entityDictionary.getType(entityClass, attribute);
            if (excludedEntities.contains(attributeClass)
                    || entityDictionary.getAttributeOrRelationAnnotation(entityClass,
                    SubscriptionField.class, attribute) == null) {
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
            if (excludedEntities.contains(relationshipClass)
                    || entityDictionary.getAttributeOrRelationAnnotation(entityClass,
                    SubscriptionField.class, relationship) == null) {
                continue;
            }

            relationshipTypes.add(relationshipClass);

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
