/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import java.util.HashSet;
import java.util.Set;

/**
 * Extracts all the physical references from a given reference tree.
 */
public class PhysicalReferenceExtractor implements ReferenceVisitor<Set<PhysicalReference>> {
    private final Set<PhysicalReference> references;
    private final MetaDataStore store;
    private final ExpressionParser parser;

    public PhysicalReferenceExtractor(MetaDataStore store) {
        this.store = store;
        this.parser = new ExpressionParser(store);
        this.references = new HashSet<>();
    }

    @Override
    public Set<PhysicalReference> visitPhysicalReference(PhysicalReference reference) {
        references.add(reference);
        return references;
    }

    @Override
    public Set<PhysicalReference> visitLogicalReference(LogicalReference reference) {
        reference.getReferences().stream()
                .map(ref -> ref.accept(this))
                .flatMap(Set::stream)
                .forEach(references::add);

        return references;
    }

    @Override
    public Set<PhysicalReference> visitJoinReference(JoinReference reference) {
        JoinPath path = reference.getPath();

        Path.PathElement firstElement = path.getPathElements().get(0);

        String fieldName = firstElement.getFieldName();
        Type<?> parentClass = firstElement.getType();

        SQLTable table = store.getTable(parentClass);
        SQLJoin join = table.getJoin(fieldName);

        parser.parse(table, join.getJoinExpression()).stream()
                .filter(ref -> ! (ref instanceof JoinReference))
                .forEach(ref -> ref.accept(this));

        return references;
    }
}
