/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.dialect.jsonapi;

import com.paiondata.elide.core.filter.dialect.ParseException;
import com.paiondata.elide.core.filter.expression.FilterExpression;

import java.util.List;
import java.util.Map;

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
     * @param apiVersion the version of the API requested.
     * @return The root of an expression abstract syntax tree parsed from both the path and the query parameters.
     * @throws ParseException if the expression cannot be parsed.
     */
    public FilterExpression parseGlobalExpression(
            String path,
            Map<String, List<String>> filterParams,
            String apiVersion) throws ParseException;
}
