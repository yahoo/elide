/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.request.Pagination;
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
    protected boolean supportsOperationType(OperationDefinition.Operation operation) {
        if (operation == OperationDefinition.Operation.SUBSCRIPTION) {
            return true;
        }
        return false;
    }

    @Override
    protected Pagination getDefaultPagination(Type<?> entityType) {
        return null;
    }
}
