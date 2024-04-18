/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLObjectType.newObject;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.RelationshipType;
import com.paiondata.elide.core.security.checks.prefab.Role;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.graphql.federation.EntitiesDataFetcher;
import com.paiondata.elide.graphql.federation.EntityTypeResolver;
import com.paiondata.elide.graphql.federation.FederationSchema;
import com.paiondata.elide.graphql.federation.FederationVersion;
import com.apollographql.federation.graphqljava.Federation;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.collections4.CollectionUtils;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.TypeResolver;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    private DataFetcher<?> dataFetcher;
    private GraphQLArgument idArgument;
    private GraphQLArgument filterArgument;
    private GraphQLArgument pageOffsetArgument;
    private GraphQLArgument pageFirstArgument;
    private GraphQLArgument sortArgument;
    private GraphQLConversionUtils generator;
    private GraphQLNameUtils nameUtils;
    private GraphQLObjectType pageInfoObject;
    private final String apiVersion;

    private Map<Type<?>, GraphQLInputObjectType> inputObjectRegistry;
    private Map<Type<?>, GraphQLObjectType> queryObjectRegistry;
    private Map<Type<?>, GraphQLObjectType> connectionObjectRegistry;
    private Set<Type<?>> excludedEntities;
    private Set<GraphQLObjectType> objectTypes;

    private Map<RelationshipOpKey, GraphQLArgument> relationshipOpArgument;

    private boolean enableFederation;
    private FederationVersion federationVersion;
    private Optional<FederationSchema> federationSchema;

    private final GraphQLFieldDefinitionCustomizer graphqlFieldDefinitionCustomizer;

    @Builder
    @Data
    public static class RelationshipOpKey {
        private final Type<?> entity;
        private final String field;
    }

    /**
     * Class constructor, constructs the custom arguments to handle mutations.
     * @param entityDictionary elide entity dictionary
     * @param nonEntityDictionary elide non-entity dictionary
     * @param dataFetcher graphQL data fetcher
     */
    public ModelBuilder(EntityDictionary entityDictionary,
                        NonEntityDictionary nonEntityDictionary,
                        ElideSettings settings,
                        DataFetcher<?> dataFetcher, String apiVersion) {
        objectTypes = new HashSet<>();
        this.generator = new GraphQLConversionUtils(entityDictionary, nonEntityDictionary);

        this.entityDictionary = entityDictionary;
        this.nameUtils = new GraphQLNameUtils(entityDictionary);
        this.dataFetcher = dataFetcher;
        this.apiVersion = apiVersion;

        GraphQLSettings graphQLSettings = settings.getSettings(GraphQLSettings.class);

        this.enableFederation = graphQLSettings.getFederation().isEnabled();
        this.federationVersion = graphQLSettings.getFederation().getVersion();
        this.graphqlFieldDefinitionCustomizer = graphQLSettings.getGraphqlFieldDefinitionCustomizer();

        if (this.enableFederation) {
            this.federationSchema = Optional
                    .of(FederationSchema.builder().version(federationVersion).imports("@key", "@shareable").build());
        } else {
            this.federationSchema = Optional.empty();
        }
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

        GraphQLObjectType.Builder pageInfoObjectBuilder = newObject()
                .name(OBJECT_PAGE_INFO)
                .field(newFieldDefinition()
                        .name("hasNextPage")
                        .type(Scalars.GraphQLBoolean))
                .field(newFieldDefinition()
                        .name("startCursor")
                        .type(Scalars.GraphQLString))
                .field(newFieldDefinition()
                        .name("endCursor")
                        .type(Scalars.GraphQLString))
                .field(newFieldDefinition()
                        .name("totalRecords")
                        .type(Scalars.GraphQLInt));
        federationSchema.ifPresent(schema -> schema.shareable().ifPresent(pageInfoObjectBuilder::withAppliedDirective));
        pageInfoObject = pageInfoObjectBuilder.build();

        objectTypes.add(pageInfoObject);

        inputObjectRegistry = new HashMap<>();
        queryObjectRegistry = new HashMap<>();
        connectionObjectRegistry = new HashMap<>();
        excludedEntities = new HashSet<>();
        relationshipOpArgument = new HashMap<>();
    }

    public void withExcludedEntities(Set<Type<?>> excludedEntities) {
        this.excludedEntities = excludedEntities;
    }

    /**
     * Gets the relationship op for a root entity.
     *
     * @param entityClass the entity class
     * @return the relationship op
     */
    public GraphQLArgument getRelationshipOp(Type<?> entityClass) {
        RelationshipOpKey key = RelationshipOpKey.builder().entity(entityClass).build();
        GraphQLArgument existing = relationshipOpArgument.get(key);
        if (existing != null) {
            return existing;
        }

        String entityName = entityDictionary.getJsonAliasFor(entityClass);
        String postfix = entityName.substring(0, 1).toUpperCase(Locale.ENGLISH) + entityName.substring(1);
        GraphQLEnumType relationshipOp = generator.classToNamedEnumType(ClassType.of(RelationshipOp.class),
                name -> name + postfix, e -> {
                    RelationshipOp op = RelationshipOp.valueOf(e.name());
                    switch (op) {
                    case FETCH:
                        return canRead(entityClass);
                    case DELETE:
                        return canDelete(entityClass);
                    case UPSERT:
                        return canCreate(entityClass);
                    case REPLACE:
                        return canCreate(entityClass) || canUpdate(entityClass) || canDelete(entityClass);
                    case REMOVE:
                        return canDelete(entityClass);
                    case UPDATE:
                        return canUpdate(entityClass);
                    }
                    throw new IllegalArgumentException("Unsupported enum value " + e.toString());
                });

        GraphQLArgument result = buildRelationshipOpArgument(relationshipOp);
        relationshipOpArgument.put(key, result);
        return result;
    }

    /**
     * Gets the relationship op for a relationship.
     *
     * @param entityClass the entity class
     * @param field the field
     * @param relationshipClass the relationship class
     * @return the relationship op
     */
    public GraphQLArgument getRelationshipOp(Type<?> entityClass, String field, Type<?> relationshipClass) {
        RelationshipOpKey key = RelationshipOpKey.builder().entity(entityClass).field(field).build();
        GraphQLArgument existing = relationshipOpArgument.get(key);
        if (existing != null) {
            return existing;
        }

        String entityName = entityDictionary.getJsonAliasFor(entityClass);
        String postfix = entityName.substring(0, 1).toUpperCase(Locale.ENGLISH) + entityName.substring(1)
                + field.substring(0, 1).toUpperCase(Locale.ENGLISH) + field.substring(1);
        GraphQLEnumType relationshipOp = generator.classToNamedEnumType(ClassType.of(RelationshipOp.class),
                name -> name + postfix, e -> {
                    RelationshipOp op = RelationshipOp.valueOf(e.name());
                    switch (op) {
                    case FETCH:
                        return canRead(entityClass, field);
                    case DELETE:
                        return canDelete(relationshipClass);
                    case UPSERT:
                        return canUpdate(entityClass, field);
                    case REPLACE:
                        return canUpdate(entityClass, field);
                    case REMOVE:
                        return canUpdate(entityClass, field);
                    case UPDATE:
                        return canUpdate(entityClass, field);
                    }
                    throw new IllegalArgumentException("Unsupported enum value " + e.toString());
                });

        GraphQLArgument result = buildRelationshipOpArgument(relationshipOp);
        relationshipOpArgument.put(key, result);
        return result;
    }

    /**
     * Builds the relationship op argument given the relationship op enum.
     *
     * @param relationshipOp the relationship op enum
     * @return the argument
     */
    private GraphQLArgument buildRelationshipOpArgument(GraphQLEnumType relationshipOp) {
        Object defaultValue = null;
        if (!relationshipOp.getValues().isEmpty()) {
            if (relationshipOp.getValue(RelationshipOp.FETCH.name()) != null) {
                defaultValue = relationshipOp.getValue(RelationshipOp.FETCH.name()).getValue();
            } else {
                defaultValue = null;
            }
        } else {
            // No operations
            return null;
        }

        return newArgument()
                .name(ARGUMENT_OPERATION)
                .type(relationshipOp)
                .defaultValueProgrammatic(defaultValue)
                .build();
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

        /* Construct root object */
        GraphQLObjectType.Builder root = newObject().name(OBJECT_QUERY);
        for (Type<?> clazz : rootClasses) {
            String entityName = entityDictionary.getJsonAliasFor(clazz);

            GraphQLArgument relationshipOpArg = getRelationshipOp(clazz);
            if (relationshipOpArg != null) {
                root.field(newFieldDefinition()
                        .name(entityName)
                        .description(EntityDictionary.getEntityDescription(clazz))
                        .argument(relationshipOpArg)
                        .argument(idArgument)
                        .argument(filterArgument)
                        .argument(sortArgument)
                        .argument(pageFirstArgument)
                        .argument(pageOffsetArgument)
                        .argument(buildInputObjectArgument(clazz, true))
                        .arguments(generator.entityArgumentToQueryObject(clazz, entityDictionary))
                        .type(buildConnectionObject(clazz)));
            }
        }


        GraphQLObjectType queryRoot = root.build();
        GraphQLObjectType mutationRoot = root.name(OBJECT_MUTATION).build();

        objectTypes.add(queryRoot);
        objectTypes.add(mutationRoot);

        /*
         * Walk the object graph (avoiding cycles) and construct the GraphQL output object types.
         */
        entityDictionary.walkEntityGraph(rootClasses, this::buildConnectionObject);

        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

        objectTypes.addAll(generator.getObjectTypes());
        for (GraphQLObjectType objectType : objectTypes) {
            String objectName = objectType.getName();
            for (GraphQLFieldDefinition fieldDefinition : objectType.getFieldDefinitions()) {
                codeRegistry.dataFetcher(
                        FieldCoordinates.coordinates(objectName, fieldDefinition.getName()),
                        dataFetcher);
            }
        }

        /* Construct the schema */
        GraphQLSchema.Builder schemaBuilder = federationSchema.isPresent()
                ? GraphQLSchema.newSchema(federationSchema.get().getSchema())
                : GraphQLSchema.newSchema();
        schemaBuilder.query(queryRoot).mutation(mutationRoot).codeRegistry(codeRegistry.build()).additionalTypes(
                new HashSet<>(CollectionUtils.union(connectionObjectRegistry.values(), inputObjectRegistry.values())));

        if (enableFederation) {
            //Enable Apollo Federation
            DataFetcher<?> entitiesDataFetcher = new EntitiesDataFetcher();
            TypeResolver entityTypeResolver = new EntityTypeResolver(this.nameUtils);
            boolean federation2 = federationVersion.intValue() >= 20;
            return Federation.transform(schemaBuilder.build())
                    .fetchEntities(entitiesDataFetcher)
                    .resolveEntityType(entityTypeResolver)
                    .setFederation2(federation2)
                    .build();
        } else {
            return schemaBuilder.build();
        }
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
                        .type(buildEdgesObject(entityClass, buildQueryObject(entityClass))))
                .field(newFieldDefinition()
                        .name("pageInfo")
                        .type(pageInfoObject))
                .build();

        objectTypes.add(connectionObject);

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

        log.trace("Building query object for {}", entityClass.getName());

        GraphQLObjectType.Builder builder = newObject()
                .name(nameUtils.toNodeName(entityClass))
                .description(EntityDictionary.getEntityDescription(entityClass));

        String id = entityDictionary.getIdFieldName(entityClass);

        /* our id types are DeferredId objects (not Scalars.GraphQLID) */
        builder.field(newFieldDefinition()
                .name(id)
                .type(GraphQLScalars.GRAPHQL_DEFERRED_ID));

        if (this.enableFederation) {
            if (entityDictionary.isRoot(entityClass)) {
                // Add @key for root entities
                federationSchema.map(schema -> schema.key(id)).ifPresent(builder::withAppliedDirective);
            }
        }

        for (String attribute : entityDictionary.getAttributes(entityClass)) {
            Type<?> attributeClass = entityDictionary.getType(entityClass, attribute);
            if (excludedEntities.contains(attributeClass)) {
                continue;
            }

            log.trace("Building query attribute {} {} with arguments {} for entity {}",
                    attribute,
                    attributeClass.getName(),
                    entityDictionary.getAttributeArguments(attributeClass, attribute).toString(),
                    entityClass.getName());

            GraphQLType attributeType =
                    generator.attributeToQueryObject(entityClass, attributeClass, attribute, dataFetcher);

            if (attributeType == null) {
                continue;
            }
            GraphQLFieldDefinition.Builder fieldDefinition = newFieldDefinition().name(attribute)
                    .arguments(generator.attributeArgumentToQueryObject(entityClass, attribute, dataFetcher))
                    .type((GraphQLOutputType) attributeType);
            if (this.graphqlFieldDefinitionCustomizer != null) {
                this.graphqlFieldDefinitionCustomizer.customize(fieldDefinition, entityClass, attributeClass, attribute,
                        dataFetcher, entityDictionary);
            }
            builder.field(fieldDefinition);
        }

        for (String relationship : entityDictionary.getElideBoundRelationships(entityClass)) {
            Type<?> relationshipClass = entityDictionary.getParameterizedType(entityClass, relationship);
            if (excludedEntities.contains(relationshipClass)) {
                continue;
            }

            String relationshipEntityName = nameUtils.toConnectionName(relationshipClass);
            RelationshipType type = entityDictionary.getRelationshipType(entityClass, relationship);
            GraphQLArgument relationshipOpArg = getRelationshipOp(entityClass, relationship, relationshipClass);
            if (relationshipOpArg != null) {
                if (type.isToOne()) {
                    GraphQLFieldDefinition.Builder fieldDefinition = newFieldDefinition().name(relationship)
                            .argument(relationshipOpArg)
                            .argument(buildInputObjectArgument(relationshipClass, false))
                            .arguments(generator.entityArgumentToQueryObject(relationshipClass, entityDictionary))
                            .type(new GraphQLTypeReference(relationshipEntityName));
                    if (this.graphqlFieldDefinitionCustomizer != null) {
                        this.graphqlFieldDefinitionCustomizer.customize(fieldDefinition, entityClass, relationshipClass,
                                relationship, dataFetcher, entityDictionary);
                    }
                    builder.field(fieldDefinition);
                } else {
                    GraphQLFieldDefinition.Builder fieldDefinition = newFieldDefinition().name(relationship)
                            .argument(relationshipOpArg)
                            .argument(filterArgument)
                            .argument(sortArgument)
                            .argument(pageOffsetArgument)
                            .argument(pageFirstArgument)
                            .argument(idArgument)
                            .argument(buildInputObjectArgument(relationshipClass, true))
                            .arguments(generator.entityArgumentToQueryObject(relationshipClass, entityDictionary))
                            .type(new GraphQLTypeReference(relationshipEntityName));
                    if (this.graphqlFieldDefinitionCustomizer != null) {
                        this.graphqlFieldDefinitionCustomizer.customize(fieldDefinition, entityClass, relationshipClass,
                                relationship, dataFetcher, entityDictionary);
                    }
                    builder.field(fieldDefinition);
                }
            }
        }

        GraphQLObjectType queryObject = builder.build();

        objectTypes.add(queryObject);

        queryObjectRegistry.put(entityClass, queryObject);
        return queryObject;
    }

    private GraphQLList buildEdgesObject(Type<?> relationClass, GraphQLOutputType entityType) {
        GraphQLObjectType edgesObject = newObject()
                .name(nameUtils.toEdgesName(relationClass))
                .field(newFieldDefinition()
                        .name("node")
                        .type(entityType))
                .build();

        objectTypes.add(edgesObject);

        return new GraphQLList(edgesObject);
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
        log.trace("Building input object for {}", clazz.getName());

        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject();
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

            log.trace("Building input attribute {} {} for entity {}",
                    attribute,
                    attributeClass.getName(),
                    clazz.getName());

            GraphQLInputType attributeType = generator.attributeToInputObject(clazz, attributeClass, attribute);

            builder.field(newInputObjectField()
                .name(attribute)
                .type(attributeType)
            );
        }
        for (String relationship : entityDictionary.getElideBoundRelationships(clazz)) {
            log.trace("Resolving relationship {} for {}", relationship, clazz.getName());
            Type<?> relationshipClass = entityDictionary.getParameterizedType(clazz, relationship);
            if (excludedEntities.contains(relationshipClass)) {
                continue;
            }

            RelationshipType type = entityDictionary.getRelationshipType(clazz, relationship);
            String relationshipEntityName = nameUtils.toInputTypeName(relationshipClass);

            if (type.isToOne()) {
                builder.field(newInputObjectField()
                        .name(relationship)
                        .type(new GraphQLTypeReference(relationshipEntityName))
                        .build());
            } else {
                builder.field(newInputObjectField()
                        .name(relationship)
                        .type(new GraphQLList(new GraphQLTypeReference(relationshipEntityName)))
                        .build());
            }
        }

        GraphQLInputObjectType constructed = builder.build();
        inputObjectRegistry.put(clazz, constructed);
        return constructed;
    }

    protected boolean isNone(String permission) {
        return "Prefab.Role.None".equalsIgnoreCase(permission) || Role.NONE_ROLE.equalsIgnoreCase(permission);
    }

    protected boolean canCreate(Type<?> type) {
        return !isNone(getCreatePermission(type));
    }

    protected boolean canRead(Type<?> type) {
        return !isNone(getReadPermission(type));
    }

    protected boolean canUpdate(Type<?> type) {
        return !isNone(getUpdatePermission(type));
    }

    protected boolean canDelete(Type<?> type) {
        return !isNone(getDeletePermission(type));
    }

    protected boolean canCreate(Type<?> type, String field) {
        return !isNone(getCreatePermission(type, field));
    }

    protected boolean canRead(Type<?> type, String field) {
        return !isNone(getReadPermission(type, field));
    }

    protected boolean canUpdate(Type<?> type, String field) {
        return !isNone(getUpdatePermission(type, field));
    }

    protected boolean canDelete(Type<?> type, String field) {
        return !isNone(getDeletePermission(type, field));
    }

    /**
     * Get the calculated {@link CreatePermission} value for the entity.
     *
     * @param clazz the entity class
     * @return the create permissions for an entity
     */
    protected String getCreatePermission(Type<?> clazz) {
        return getPermission(clazz, CreatePermission.class);
    }

    /**
     * Get the calculated {@link ReadPermission} value for the entity.
     *
     * @param clazz the entity class
     * @return the read permissions for an entity
     */
    protected String getReadPermission(Type<?> clazz) {
        return getPermission(clazz, ReadPermission.class);
    }

    /**
     * Get the calculated {@link UpdatePermission} value for the entity.
     *
     * @param clazz the entity class
     * @return the update permissions for an entity
     */
    protected String getUpdatePermission(Type<?> clazz) {
        return getPermission(clazz, UpdatePermission.class);
    }

    /**
     * Get the calculated {@link DeletePermission} value for the entity.
     *
     * @param clazz the entity class
     * @return the delete permissions for an entity
     */
    protected String getDeletePermission(Type<?> clazz) {
        return getPermission(clazz, DeletePermission.class);
    }

    /**
     * Get the calculated {@link CreatePermission} value for the relationship.
     *
     * @param clazz the entity class
     * @param field the field to inspect
     * @return the create permissions for the relationship
     */
    protected String getCreatePermission(Type<?> clazz, String field) {
        return getPermission(clazz, field, CreatePermission.class);
    }

    /**
     * Get the calculated {@link ReadPermission} value for the relationship.
     *
     * @param clazz the entity class
     * @param field the field to inspect
     * @return the read permissions for the relationship
     */
    protected String getReadPermission(Type<?> clazz, String field) {
        return getPermission(clazz, field, ReadPermission.class);
    }

    /**
     * Get the calculated {@link UpdatePermission} value for the relationship.
     *
     * @param clazz the entity class
     * @param field the field to inspect
     * @return the update permissions for the relationship
     */
    protected String getUpdatePermission(Type<?> clazz, String field) {
        return getPermission(clazz, field, UpdatePermission.class);
    }

    /**
     * Get the calculated {@link DeletePermission} value for the relationship.
     *
     * @param clazz the entity class
     * @param field the field to inspect
     * @return the delete permissions for the relationship
     */
    protected String getDeletePermission(Type<?> clazz, String field) {
        return getPermission(clazz, field, DeletePermission.class);
    }

    protected String getPermission(Type<?> clazz, Class<? extends Annotation> permission) {
        ParseTree parseTree = entityDictionary.getPermissionsForClass(clazz, permission);
        if (parseTree != null) {
            return parseTree.getText();
        }
        return null;
    }

    protected String getPermission(Type<?> clazz, String field, Class<? extends Annotation> permission) {
        ParseTree parseTree = entityDictionary.getPermissionsForField(clazz, field, permission);
        if (parseTree != null) {
            return parseTree.getText();
        }
        return null;
    }
}
