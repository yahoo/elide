/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.expression;

/**
 * A reference to a column in a column or join expression.
 */
public interface Reference {

    /**
     * Accepts a visitor that walks the reference AST.
     * @param visitor The visitor
     * @param <T> The return type of the visitor
     * @return a T
     */
    <T> T accept(ReferenceVisitor<T> visitor);
}
