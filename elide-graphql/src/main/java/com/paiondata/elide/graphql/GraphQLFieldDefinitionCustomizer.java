/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.Type;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;

/**
 * Customizer for GraphQLFieldDefinition.
 */
@FunctionalInterface
public interface GraphQLFieldDefinitionCustomizer {
    /**
     * Customize the field definition.
     *
     * @param fieldDefinition the field definition to customize
     * @param parentClass the entity class
     * @param attributeClass the attribute class
     * @param attribute the attribute name
     * @param fetcher the fetcher
     * @param entityDictionary the entity dictionary
     */
    void customize(GraphQLFieldDefinition.Builder fieldDefinition, Type<?> parentClass, Type<?> attributeClass,
            String attribute, DataFetcher<?> fetcher, EntityDictionary entityDictionary);
}
