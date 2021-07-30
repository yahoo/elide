/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions;

import static graphql.introspection.Introspection.__Type;
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
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.PropertyDataFetcher;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SubscriptionModelBuilder {
    private Map<Type<?>, GraphQLType> queryObjectRegistry;
    private final String apiVersion;
    private GraphQLConversionUtils generator;
    private GraphQLNameUtils nameUtils;
    private EntityDictionary entityDictionary;
    private DataFetcher dataFetcher;
    private GraphQLArgument filterArgument;
    private Set<Type<?>> excludedEntities;

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
        for (Type<?> clazz : rootClasses) {
            String entityName = entityDictionary.getJsonAliasFor(clazz);
            root.field(newFieldDefinition()
                    .name(entityName)
                    .description(EntityDictionary.getEntityDescription(clazz))
                    .argument(filterArgument)
                    .type(buildConnectionObject(clazz)));
        }

        GraphQLObjectType queryRoot = root.build();

        /*
         * Walk the object graph (avoiding cycles) and construct the GraphQL output object types.
         */
        entityDictionary.walkEntityGraph(rootClasses, this::buildConnectionObject);

        /* Construct the schema */
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .subscription(queryRoot)

                //Workaround for bug in GraphQL java (https://github.com/graphql-java/graphql-java/issues/2493).
                //Check if schema introspection is trying to user our default data fetcher.  If so, don't let it.
                .codeRegistry(GraphQLCodeRegistry.newCodeRegistry()
                        .defaultDataFetcher((env) -> {
                            GraphQLType type = env.getFieldDefinition().getType();
                            if (type instanceof GraphQLNonNull) {
                                type = ((GraphQLNonNull) type).getWrappedType();
                            }
                            if (type.equals(__Type) && env.getFieldDefinition().getName().equals("type")) {
                                return PropertyDataFetcher.fetching("type");
                            }
                            return dataFetcher;
                        })
                        .build())
                .additionalTypes(Sets.newHashSet(queryObjectRegistry.values()))
                .build();

        return schema;
    }

    /**
     * Builds a graphQL output object from an entity class.
     * @param entityClass The class to use to construct the output object.
     * @return The graphQL object
     */
    private GraphQLType buildQueryObjectStub(Type<?> entityClass) {
        if (queryObjectRegistry.containsKey(entityClass)) {
            return queryObjectRegistry.get(entityClass);
        }

        log.debug("Building subscription object for {}", entityClass.getName());

        GraphQLObjectType.Builder builder = newObject()
                .name(nameUtils.toNodeName(entityClass))
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

        GraphQLObjectType queryObject = builder.build();
        queryObjectRegistry.put(entityClass, queryObject);
        return queryObject;
    }
}
