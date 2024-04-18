/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.graphql.annotation.GraphQLDescription;

/**
 * Default GraphQL Field Definition customizer.
 */
public class DefaultGraphQLFieldDefinitionCustomizer
        extends AnnotationGraphQLFieldDefinitionDescriptionCustomizer<GraphQLDescription> {

    public static final DefaultGraphQLFieldDefinitionCustomizer INSTANCE =
            new DefaultGraphQLFieldDefinitionCustomizer();

    public DefaultGraphQLFieldDefinitionCustomizer() {
        super(GraphQLDescription.class, annotation -> annotation.value());
    }
}
