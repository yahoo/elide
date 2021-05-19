/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static java.util.Collections.emptySet;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import java.util.HashSet;
import java.util.Set;

/**
 * Extracts all the logical references from a given reference tree.  This extractor
 * does not descend into joins.
 */
public class LogicalReferenceExtractor implements ReferenceVisitor<Set<LogicalReference>> {
    private final Set<LogicalReference> references;
    private final MetaDataStore store;
    private final ExpressionParser parser;

    public LogicalReferenceExtractor(MetaDataStore store) {
        this.store = store;
        this.parser = new ExpressionParser(store);
        this.references = new HashSet<>();
    }

    @Override
    public Set<LogicalReference> visitPhysicalReference(PhysicalReference reference) {
        return new HashSet<>();
    }

    @Override
    public Set<LogicalReference> visitLogicalReference(LogicalReference reference) {
        references.add(reference);

        reference.getReferences().stream()
                .map(ref -> ref.accept(this))
                .flatMap(Set::stream)
                .forEach(references::add);

        return references;
    }

    @Override
    public Set<LogicalReference> visitJoinReference(JoinReference reference) {
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

    @Override
    public Set<LogicalReference> visitColumnArgReference(ColumnArgReference reference) {
        return emptySet();
    }

    @Override
    public Set<LogicalReference> visitTableArgReference(TableArgReference reference) {
        return emptySet();
    }
}
