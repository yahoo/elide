/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NonNullType;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.OperationDefinition;
import graphql.language.StringValue;
import graphql.language.Type;
import graphql.language.Value;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Class that contains variables provided in graphql request and can resolve variables based on
 * {@link graphql.language.OperationDefinition} scope.
 * 1. variables defined in request is global
 * 2. variables defined in each operation is operation-scoped
 */
class VariableResolver {
    private final Map<String, Object> requestVariables;
    private final Map<String, Object> scopeVariables = new HashMap<>();

    VariableResolver(Map<String, Object> variables) {
        this.requestVariables = new HashMap<>(variables);
    }

    /**
     * Start a new variable scope for operation, clear all variables in the previous scope and add request variables
     * into every new scope.
     *
     * @param operation operation definition
     */
    public void newScope(OperationDefinition operation) {
        this.scopeVariables.clear();
        this.scopeVariables.putAll(requestVariables);
        operation.getVariableDefinitions().forEach(this::addVariable);
    }

    /**
     * Resolve {@link VariableDefinition} and store result in the variable map.
     * We don't need to worry about resolving graphql {@link graphql.language.TypeName} here because Elide-core
     * knows the correct type of each field/argument.
     *
     * @param definition definition to resolve
     */
    private void addVariable(VariableDefinition definition) {
        Type variableType = definition.getType();
        String variableName = definition.getName();
        Value defaultValue = definition.getDefaultValue();

        if (defaultValue == null) {
            if (variableType instanceof NonNullType && scopeVariables.get(variableName) == null) {
                // value of non-null variable must be resolvable
                throw new BadRequestException("Undefined non-null variable " + variableName);
            }
            // this would put 'null' for this variable if it is not stored in the map
            scopeVariables.put(variableName, scopeVariables.get(variableName));
        } else {
            if (!scopeVariables.containsKey(variableName)) {
                // create a new variable with default value
                scopeVariables.put(variableName, resolveValue(defaultValue));
            }
        }
    }

    /**
     * Resolve the real value of a GraphQL {@link Value} object. Use variables in request if necessary.
     *
     * @param value requested variable value
     * @return resolved value of given variable
     */
    public Object resolveValue(Value value) {
        return resolveValue(value, Optional.empty());
    }

    /**
     * Resolve the real value of a GraphQL {@link Value} object. Use variables in request if necessary.
     *
     * @param value requested variable value
     * @param resolveTo the Elide type to resolve to
     * @return resolved value of given variable
     */
    public Object resolveValue(Value value, Optional<com.yahoo.elide.core.type.Type<?>> resolveTo) {
        if (value instanceof BooleanValue) {
            return ((BooleanValue) value).isValue();
        }
        if (value instanceof EnumValue) {
            if (resolveTo.isPresent()) {
                return CoerceUtil.coerce(((EnumValue) value).getName().toUpperCase(Locale.ROOT), resolveTo.get());
            }
            throw new BadRequestException("Enum value is not supported.");
        }
        if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue();
        }
        if (value instanceof IntValue) {
            return ((IntValue) value).getValue();
        }
        if (value instanceof NullValue) {
            return null;
        }
        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        if (value instanceof ObjectValue) {
            return ((ObjectValue) value).getObjectFields().stream()
                    .collect(Collectors.toMap(ObjectField::getName, ObjectField::getValue));
        }
        if (value instanceof ArrayValue) {
            return ((ArrayValue) value).getValues().stream()
                    .map(this::resolveValue)
                    .collect(Collectors.toList());
        }
        if (value instanceof VariableReference) {
            String variableName = ((VariableReference) value).getName();
            if (!scopeVariables.containsKey(variableName)) {
                throw new BadRequestException("Can't resolve variable reference " + variableName);
            }

            return scopeVariables.get(variableName);
        }
        throw new BadRequestException("Unknown variable value type " + value.getClass());
    }
}
