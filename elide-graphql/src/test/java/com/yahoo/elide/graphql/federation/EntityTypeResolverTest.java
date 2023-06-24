/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.graphql.GraphQLNameUtils;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.containers.NodeContainer;
import com.yahoo.elide.models.Book;

import org.junit.jupiter.api.Test;

import graphql.TypeResolutionEnvironment;
import graphql.execution.TypeResolutionParameters;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

/**
 * Test for EntityTypeResolver.
 */
class EntityTypeResolverTest {

    @Test
    void getTypeNodeContainer() {
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        entityDictionary.bindEntity(Book.class);
        GraphQLNameUtils nameUtils = new GraphQLNameUtils(entityDictionary);
        EntityTypeResolver entityTypeResolver = new EntityTypeResolver(nameUtils);
        ElideSettings elideSettings = ElideSettings.builder().entityDictionary(entityDictionary).build();
        Route route = Route.builder().build();
        PersistentResource<Book> book = new PersistentResource<>(new Book(), "1",
                GraphQLRequestScope.builder().elideSettings(elideSettings).route(route).build());
        NodeContainer value = new NodeContainer(book);
        GraphQLObjectType bookType = GraphQLObjectType.newObject().name(nameUtils.toOutputTypeName(ClassType.of(Book.class))).build();
        GraphQLSchema schema = mock(GraphQLSchema.class);
        when(schema.getObjectType(bookType.getName())).thenReturn(bookType);
        TypeResolutionEnvironment env = TypeResolutionParameters.newParameters().value(value).schema(schema).build();
        GraphQLObjectType objectType = entityTypeResolver.getType(env);
        assertEquals(bookType.getName(), objectType.getName());
    }

    @Test
    void getType() {
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        entityDictionary.bindEntity(Book.class);
        GraphQLNameUtils nameUtils = new GraphQLNameUtils(entityDictionary);
        EntityTypeResolver entityTypeResolver = new EntityTypeResolver(nameUtils);
        Book value = new Book();
        GraphQLObjectType bookType = GraphQLObjectType.newObject().name(nameUtils.toOutputTypeName(ClassType.of(Book.class))).build();
        GraphQLSchema schema = mock(GraphQLSchema.class);
        when(schema.getObjectType(bookType.getName())).thenReturn(bookType);
        TypeResolutionEnvironment env = TypeResolutionParameters.newParameters().value(value).schema(schema).build();
        GraphQLObjectType objectType = entityTypeResolver.getType(env);
        assertEquals(bookType.getName(), objectType.getName());
    }
}
