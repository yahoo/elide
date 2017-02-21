package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class ModelBuilder {
    private EntityDictionary dictionary;
    private DataFetcher dataFetcher;
    private GraphQLArgument relationshipOpArg;
    private GraphQLArgument idArgument;

    ModelBuilder(EntityDictionary dictionary, DataFetcher dataFetcher) {
        this.dictionary = dictionary;
        this.dataFetcher = dataFetcher;

        relationshipOpArg = GraphQLArgument.newArgument()
                .name("op")
                .type(RelationshipOp.toGraphQLType())
                .defaultValue(RelationshipOp.FETCH)
                .build();

        idArgument = GraphQLArgument.newArgument()
                .name("id")
                .type(Scalars.GraphQLString)
                .build();
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
        Set<Class<?>> visited = new HashSet<>(rootClasses);

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
        Queue < Class < ?>> toVisit = new ArrayDeque<>(rootClasses);
        while (! toVisit.isEmpty()) {
            Class<?> clazz = toVisit.remove();
            visited.add(clazz);

            for (String relationship : dictionary.getRelationships(clazz)) {
                Class<?> relationshipClass = dictionary.getType(clazz, relationship);
                if (!visited.contains(relationshipClass)) {
                    toVisit.add(clazz);
                }
            }
        }

        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(root)
                .mutation(root)
                .build();

        return schema;
    }

    /**
     * Builds a graphQL output object from an entity class.
     * @param entityClass The class to use to construct the output object.
     * @return The graphQL object
     */
    private GraphQLObjectType buildQueryObject(Class<?> entityClass) {
        String entityName = dictionary.getJsonAliasFor(entityClass);

        GraphQLObjectType.Builder builder = newObject();
        builder.name(entityName);

        String id = dictionary.getIdFieldName(entityClass);
        builder.field(newFieldDefinition()
                    .name(id)
                    .type(Scalars.GraphQLID));

        for (String attribute : dictionary.getAttributes(entityClass)) {
            Class<?> attributeClass = dictionary.getType(entityClass, attribute);
            builder.field(newFieldDefinition()
                    .name(attribute)
                    .dataFetcher(dataFetcher)
                    .type((GraphQLOutputType) classToType(attributeClass))
            );
        }

        for (String relationship : dictionary.getRelationships(entityClass)) {
            Class<?> relationshipClass = dictionary.getType(entityClass, relationship);

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

        return builder.build();
    }

    /**
     * Wraps a constructed GraphQL Input Object in an argument.
     * @param entityClass - The class to construct the input object from.
     * @param asList Whether or not the argument is a single instance or a list.
     * @return The constructed argument.
     */
    private GraphQLArgument buildInputObjectArgument(Class<?> entityClass, boolean asList) {
        GraphQLInputObjectType argumentType = buildInputObject(entityClass, new HashSet<GraphQLInputObjectType>());

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
     * @param visited Entities already visited
     * @return Constructed graphql input object type.
     */
    private GraphQLInputObjectType buildInputObject(Class<?> entityClass, Set<GraphQLInputObjectType> visited) {
        String entityName = dictionary.getJsonAliasFor(entityClass);

        GraphQLInputObjectType.Builder builder = newInputObject();
        builder.name(entityName);

        String id = dictionary.getIdFieldName(entityClass);

        builder.field(newInputObjectField()
                .name(id)
                .type(Scalars.GraphQLID));

        for (String attribute : dictionary.getAttributes(entityClass)) {
            Class<?> attributeClass = dictionary.getType(entityClass, attribute);
            builder.field(newInputObjectField()
                            .name(attribute)
                            .type((GraphQLInputType) classToType(attributeClass))
            );
        }

        for (String relationship : dictionary.getRelationships(entityClass)) {
            Class<?> relationshipClass = dictionary.getType(entityClass, relationship);

            RelationshipType type = dictionary.getRelationshipType(entityClass, relationship);

            //TODO - this is inadequate cycle protection but should cover the common cases for now.
            if (visited.contains(relationshipClass)) {
                continue;
            }

            if (type.isToOne()) {
                builder.field(newInputObjectField()
                                .name(relationship)
                                .type(buildInputObject(relationshipClass, visited))
                );
            } else {
                builder.field(newInputObjectField()
                                .name(relationship)
                                .type(new GraphQLList(buildInputObject(relationshipClass, visited)))
                );
            }
        }

        return builder.build();
    }

    /**
     * Converts any non-entity type to a GraphQLType
     * @param clazz - the non-entity type.
     * @return the GraphQLType
     */
    private GraphQLType classToType(Class<?> clazz) {
        if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return Scalars.GraphQLInt;
        } else if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            return Scalars.GraphQLBoolean;
        } else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            return Scalars.GraphQLLong;
        } else if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            return Scalars.GraphQLFloat;
        } else if (clazz.equals(short.class) || clazz.equals(Short.class)) {
            return Scalars.GraphQLShort;
        } else if (clazz.equals(String.class)) {
            return Scalars.GraphQLString;
        }

        //TODO - handle collections & embedded entities

        return null;
    }
}
