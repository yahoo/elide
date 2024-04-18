/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.federation;

import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.graphql.GraphQLNameUtils;
import com.paiondata.elide.graphql.containers.PersistentResourceContainer;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

/**
 * Entity Type Resolver for Apollo Federation.
 */
public class EntityTypeResolver implements TypeResolver {

    private final GraphQLNameUtils nameUtils;

    public EntityTypeResolver(GraphQLNameUtils nameUtils) {
        this.nameUtils = nameUtils;
    }

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        final Object src = env.getObject();
        String objectType;
        if (src instanceof PersistentResourceContainer nodeContainer) {
            objectType = nameUtils.toOutputTypeName(nodeContainer.getPersistentResource().getResourceType());
        } else {
            objectType = nameUtils.toOutputTypeName(ClassType.of(src.getClass()));
        }
        return env.getSchema().getObjectType(objectType);
    }
}
