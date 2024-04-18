/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.expression;

/**
 * Walks a reference abstract syntax tree and builds a T.
 * @param <T> The type the visitor constructs/returns.
 */
public interface ReferenceVisitor<T> {

    /**
     * Visits a physical reference.
     * @param reference The physical reference
     * @return a type T.
     */
    T visitPhysicalReference(PhysicalReference reference);

    /**
     * Visits a logical reference.
     * @param reference The logical reference
     * @return a type T.
     */
    T visitLogicalReference(LogicalReference reference);

    /**
     * Visits a join reference.
     * @param reference The join reference
     * @return a type T.
     */
    T visitJoinReference(JoinReference reference);

    /**
     * Visits a column argument reference.
     * @param reference The column argument reference
     * @return a type T.
     */
    T visitColumnArgReference(ColumnArgReference reference);

    /**
     * Visits a table argument reference.
     * @param reference The table argument reference
     * @return a type T.
     */
    T visitTableArgReference(TableArgReference reference);
}
