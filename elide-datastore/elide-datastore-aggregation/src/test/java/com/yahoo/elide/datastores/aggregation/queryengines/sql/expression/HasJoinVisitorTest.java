/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import org.junit.jupiter.api.Test;

public class HasJoinVisitorTest {

    @Test
    void testHasJoin() {
        Queryable source = mock(Queryable.class);
        JoinPath path = mock(JoinPath.class);
        ColumnProjection column = mock(ColumnProjection.class);
        Reference reference = LogicalReference.builder()
                .source(source)
                .column(column)
                .reference(PhysicalReference.builder()
                        .name("foo")
                        .source(source)
                        .build())
                .reference(JoinReference
                        .builder()
                        .source(source)
                        .path(path)
                        .reference(PhysicalReference.builder()
                                .name("bar")
                                .source(source)
                                .build())
                        .build())
                .build();

        assertTrue(reference.accept(new HasJoinVisitor()));
    }

    @Test
    void testLogicalColumnNoJoin() {
        Queryable source = mock(Queryable.class);
        ColumnProjection column = mock(ColumnProjection.class);
        Reference reference = LogicalReference.builder()
                .source(source)
                .column(column)
                .reference(PhysicalReference.builder()
                        .name("foo")
                        .source(source)
                        .build())

                .build();

        assertFalse(reference.accept(new HasJoinVisitor()));
    }

    @Test
    void testPhysicalColumnNoJoin() {
        Queryable source = mock(Queryable.class);
        Reference reference = PhysicalReference.builder()
                .name("foo")
                .source(source)
                .build();

        assertFalse(reference.accept(new HasJoinVisitor()));
    }
}
