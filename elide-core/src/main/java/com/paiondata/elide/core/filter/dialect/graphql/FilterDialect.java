/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.filter.dialect.graphql;

import com.paiondata.elide.core.filter.dialect.ParseException;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.type.Type;

import java.util.Set;

/**
 * GraphQL Dialect for parsing filter API parameters into Filter Expressions.
 */
public interface FilterDialect {

    /**
     * Parses a graphQL collection filter parameter and converts it into a FilterExpression.
     * @param entityClass The model type of the collection.
     * @param attributes The requested attributes, their aliases, and arguments for the given entity model.
     * @param filterText The filter string to parse.
     * @param apiVersion The API version.
     * @return A filter expression.
     * @throws ParseException If the filter text is invalid.
     */
    FilterExpression parse(Type<?> entityClass,
                           Set<Attribute> attributes,
                           String filterText,
                           String apiVersion) throws ParseException;
}
