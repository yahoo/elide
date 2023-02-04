/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import com.yahoo.elide.datastores.aggregation.query.Queryable;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A reference to a column in the database: "$revenue".
 */
@Value
@Builder
public class PhysicalReference implements Reference {
    @NonNull
    private Queryable source;

    @NonNull
    private String name;

    @Override
    public <T> T accept(ReferenceVisitor<T> visitor) {
        return visitor.visitPhysicalReference(this);
    }
}
