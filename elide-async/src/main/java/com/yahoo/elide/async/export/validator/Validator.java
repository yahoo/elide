/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.validator;

import graphql.language.Field;

/**
 * Utility interface used to validate Entity Projections.
 */
public interface Validator {

    /**
     * Validates the EntityProjection.
     * @param entityType Class of the Entity to be validated.
     * @param field GraphQL Field.
     */
    public void validateProjection(Class<?> entityType, Field field);
}
