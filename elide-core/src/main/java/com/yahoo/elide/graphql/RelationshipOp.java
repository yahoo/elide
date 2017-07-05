/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

/**
 * Specifies how a relationship should be modified.
 */
public enum RelationshipOp {
    FETCH,
    DELETE,
    UPSERT,
    REPLACE,
    REMOVE
}
