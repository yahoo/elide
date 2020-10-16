/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.dialect.graphql;

import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;

import java.util.Map;

/**
 * GraphQL Dialect for parsing filter API parameters into Filter Expressions.
 */
public interface FilterDialect {

    /**
     * Parses a graphQL collection filter parameter and converts it into a FilterExpression.
     * @param entityClass The model type of the collection.
     * @param aliasMap A map of alias to field names from GraphQL.
     * @param filterText The filter string to parse.
     * @param apiVersion The API version.
     * @return A filter expression.
     * @throws ParseException If the filter text is invalid.
     */
    FilterExpression parse(Class<?> entityClass,
                           Map<String, String> aliasMap,
                           String filterText,
                           String apiVersion) throws ParseException;
}
