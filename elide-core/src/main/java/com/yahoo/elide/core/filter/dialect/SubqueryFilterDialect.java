/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import com.yahoo.elide.core.filter.expression.FilterExpression;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;

/**
 * Parses filters which are bound to a particular entity type.  Whenever a collection of entities
 * is returned by Elide, the corresponding filters for the collection's type are applied.
 *
 * For example, imagine two filters:
 *  - books.title like '%Foo%'
 *  - publisher.name = 'Acme Inc.'
 *
 * Whenever a collection of type 'book' is referenced, the first filter is applied.
 * Whenever a collection of type 'publisher' is referenced, the second filter is applied.
 *
 * This dialect is invoked whenever a collection of elements is returned via Elide.  This includes:
 *  - GET on a collection whose type is referenced in the filter
 *  - GET on a relationship whose type is referenced in the filter
 *  - A compound document includes a relationship whose type is referenced in the filter
 */
public interface SubqueryFilterDialect {
    /**
     * @param path The URL path
     * @param filterParams The subset of queryParams that start with 'filter'
     * @return The root of an expression abstract syntax tree parsed from both the path and the query parameters.
     */
    public Map<String, FilterExpression> parseTypedExpression(String path, MultivaluedMap<String, String> filterParams)
            throws ParseException;
}
