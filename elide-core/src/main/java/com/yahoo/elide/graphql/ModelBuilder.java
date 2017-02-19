package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import graphql.Scalars;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class ModelBuilder {
    private EntityDictionary dictionary;

    ModelBuilder(EntityDictionary dictionary ) {
        this.dictionary = dictionary;
    }

    GraphQLSchema build() {
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
                    .type(new GraphQLList(buildQueryObject(clazz))));
        }


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

    GraphQLObjectType buildQueryObject(Class<?> entityClass) {
        String entityName = dictionary.getJsonAliasFor(entityClass);

        GraphQLObjectType.Builder builder = newObject();
        builder.name(entityName);

        List<String> attributes = dictionary.getAttributes(entityClass);

        String id = dictionary.getIdFieldName(entityClass);
        builder.field(newFieldDefinition()
                    .name(id)
                    .type(Scalars.GraphQLID));

        for (String attribute : attributes) {
            Class<?> attributeClass = dictionary.getType(entityClass, attribute);
            builder.field(newFieldDefinition()
                    .name(attribute)
                    .type(classToType(attributeClass))
            );
        }

        List<String> relationships = dictionary.getRelationships(entityClass);

        for (String relationship : relationships) {
            Class<?> relationshipClass = dictionary.getType(entityClass, relationship);

            String relationshipEntityName = dictionary.getJsonAliasFor(relationshipClass);

            builder.field(newFieldDefinition()
                    .name(relationship)
                    .type(new GraphQLTypeReference(relationshipEntityName))
            );
        }

        return builder.build();
    }


    GraphQLOutputType classToType(Class<?> clazz) {
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

        return null;
    }
}
