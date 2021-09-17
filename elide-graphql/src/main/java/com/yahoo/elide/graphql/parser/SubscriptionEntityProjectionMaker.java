/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.type.Type;

import graphql.language.OperationDefinition;

import java.util.Map;

public class SubscriptionEntityProjectionMaker extends GraphQLEntityProjectionMaker {
    public SubscriptionEntityProjectionMaker(
            ElideSettings elideSettings,
            Map<String, Object> variables,
            String apiVersion) {
        super(elideSettings, variables, apiVersion);
    }

    @Override
    protected Type<?> getRootEntity(String entityName, String apiVersion) {
        String [] suffixes = {"Added", "Deleted", "Updated"};

        for (String suffix : suffixes) {
            if (entityName.endsWith(suffix)) {
                entityName = entityName.substring(0, entityName.length() - suffix.length());
                break;
            }
        }

        return entityDictionary.getEntityClass(entityName, apiVersion);
    }

    @Override
    protected boolean supportsOperationType(OperationDefinition.Operation operation) {
        if (operation != OperationDefinition.Operation.MUTATION) {
            return true;
        }
        return false;
    }
}
