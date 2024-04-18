/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.expression;


import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Handlebar Reference to a column argument.
 * eg: {{$$column.args.argName}}
 */
@Value
@Builder
public class ColumnArgReference implements Reference {

    @NonNull
    private String argName;

    @Override
    public <T> T accept(ReferenceVisitor<T> visitor) {
        return visitor.visitColumnArgReference(this);
    }
}
