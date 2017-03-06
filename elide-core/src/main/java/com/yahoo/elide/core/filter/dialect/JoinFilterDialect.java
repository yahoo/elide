/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import com.yahoo.elide.core.filter.expression.FilterExpression;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Parses a filter for one or more entity types that results in a join between them.
 * For example, the filter (books.title like '%Foo%' AND books.publisher.name = 'Acme Inc.')
 * would require a join between 'book' and 'publisher'.  The resulting join is filtered by
 * both predicates.
 * <p>
 * This filter dialect is invoked whenever the first entity is loaded from the DataStoreTransaction.
 * For example, the above filter on '/books' would be invoked when 'book' is loaded from the DataStoreTransaction.
 */
public interface JoinFilterDialect {
    /**
     * Join filters must be able to parse global expressions.
     *
     * @param path the URL path
     * @param filterParams the subset of query parameters that start with 'filter'
     * @return The root of an expression abstract syntax tree parsed from both the path and the query parameters.
     * @throws ParseException if the expression cannot be parsed.
     */
    public FilterExpression parseGlobalExpression(
            String path,
            MultivaluedMap<String, String> filterParams) throws ParseException;
}
