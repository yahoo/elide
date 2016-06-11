/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.strategy;

import com.yahoo.elide.core.filter.expression.Expression;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Parses a filter for one or more entity types that results in a join between them.
 * For example, the filter (books.title like '%Foo%' AND books.publisher.name = 'Acme Inc.')
 * would require a join between 'book' and 'publisher'.  The resulting join is filtered by
 * both predicates.
 *
 * This filter strategy is invoked whenever the first entity is loaded from the DataStoreTransaction.
 * For example, the above filter on '/books' would be invoked when 'book' is loaded from the DataStoreTransaction.
 */
public interface JoinFilterStrategy {
    /**
     * @param path the URL path
     * @param queryParams the subset of query parameters that start with 'filter'
     * @return
     * @throws ParseException
     */
    public Expression parseGlobalExpression(
            String path,
            MultivaluedMap<String, String> queryParams) throws ParseException;
}
