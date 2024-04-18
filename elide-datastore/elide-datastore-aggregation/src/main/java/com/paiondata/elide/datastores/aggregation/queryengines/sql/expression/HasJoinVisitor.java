/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.expression;

/**
 * Determines if the reference has at least one join.
 */
public class HasJoinVisitor implements ReferenceVisitor<Boolean> {
    @Override
    public Boolean visitPhysicalReference(PhysicalReference reference) {
        return false;
    }

    @Override
    public Boolean visitLogicalReference(LogicalReference reference) {
        return reference.getReferences().stream().anyMatch(ref -> ref.accept(this));
    }

    @Override
    public Boolean visitJoinReference(JoinReference reference) {
        return true;
    }

    @Override
    public Boolean visitColumnArgReference(ColumnArgReference reference) {
        return false;
    }

    @Override
    public Boolean visitTableArgReference(TableArgReference reference) {
        return false;
    }
}
