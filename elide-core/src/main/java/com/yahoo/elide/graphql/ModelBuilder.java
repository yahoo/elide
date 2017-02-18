package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Set;
import java.util.stream.Collectors;

public class ModelBuilder {
    private EntityDictionary dictionary;

    ModelBuilder(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    GraphQLSchema build() {
        Set<Class<?>> allClasses = dictionary.getBindings();
        if (allClasses.isEmpty()) {
            throw new IllegalArgumentException("None of the provided classes are exported by Elide");
        }

        Set<Class<?>> rootClasses =  allClasses.stream().filter(dictionary::isRoot).collect(Collectors.toSet());
    }

    GraphQLObjectType buildQueryObject(Class<?> entityClass) {
        GraphQLObjectType object = newObject();

    }
}
