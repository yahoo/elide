/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import lombok.Builder;
import lombok.Value;

/**
 * A reference to a column in another table ("author.books.title").
 */
@Value
@Builder
public class JoinReference implements Reference {

    private Reference reference;
    private JoinPath path;

    @Override
    public <T> T accept(ReferenceVisitor<T> visitor) {
        return visitor.visitJoinReference(this);
    }
}
