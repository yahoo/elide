/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import java.util.Map;

/**
 * Visits a query object hierarchy to construct a type T.  Useful for building translators, validators, etc.
 * @param <T> The type the visitor returns after walking the AST.
 */
public interface QueryVisitor<T> {

    /**
     * Visit the query node.
     * @param query The query.
     * @return The type T.
     */
    public T visitQuery(Query query);

    /**
     * Visit the table node.
     * @param table The table.
     * @return The type T.
     */
    public T visitQueryable(Queryable table);

    /**
     * Visit the table node using a context object.
     * @param table Queryable
     * @param context A Map object
     * @return The type T.
     */
    default T visitQueryable(Queryable table, Map<String, Object> context) {
        return visitQueryable(table);
    }
}
