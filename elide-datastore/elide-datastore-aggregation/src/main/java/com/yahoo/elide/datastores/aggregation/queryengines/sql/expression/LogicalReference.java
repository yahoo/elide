/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * A reference to a logical column in the same table.
 */
@Value
@Builder
public class LogicalReference implements Reference {

    @Singular
    private List<Reference> references;

    @NonNull
    private Queryable source;

    @NonNull
    private ColumnProjection column;

    @Override
    public <T> T accept(ReferenceVisitor<T> visitor) {
        return visitor.visitLogicalReference(this);
    }
}
