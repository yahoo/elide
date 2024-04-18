/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.expression;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.aggregation.core.JoinPath;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extracts all the references of a particular type from the reference AST.
 * @param <T> The reference type to extract.
 */
public class ReferenceExtractor<T extends Reference> implements ReferenceVisitor<Set<T>> {

    private final Class<T> referenceType;
    private final Set<T> references;
    private MetaDataStore metaDataStore;
    private Set<SQLJoin> visitedJoins;
    private ExpressionParser parser;
    private Mode mode;

    public enum Mode {
        ALL,
        SAME_QUERY,
        SAME_COLUMN,
    }

    /**
     * Constructor.
     * @param referenceType The reference type to extract.
     * @param metaDataStore Metadata store.
     */
    public ReferenceExtractor(Class<T> referenceType, MetaDataStore metaDataStore) {
        this(referenceType, metaDataStore, Mode.ALL);
    }

    /**
     * Constructor.
     * @param referenceType The reference type to extract.
     * @param metaDataStore Metadata store.
     * @param mode {@link Mode}.
     */
    public ReferenceExtractor(Class<T> referenceType, MetaDataStore metaDataStore, Mode mode) {
        this.mode = mode;
        this.referenceType = referenceType;
        this.references = new LinkedHashSet<>();
        this.metaDataStore = metaDataStore;
        this.parser = new ExpressionParser(metaDataStore);
        this.visitedJoins = new HashSet<>();
    }

    @Override
    public Set<T> visitPhysicalReference(PhysicalReference reference) {
        if (referenceType.equals(PhysicalReference.class)) {
            references.add((T) reference);
        }

        return references;
    }

    @Override
    public Set<T> visitLogicalReference(LogicalReference reference) {
        if (referenceType.equals(LogicalReference.class)) {
            references.add((T) reference);
        }

        if (mode != Mode.SAME_COLUMN) {
            reference.getReferences().stream()
                            .map(ref -> ref.accept(this))
                            .flatMap(Set::stream)
                            .forEach(references::add);
        }

        return references;
    }

    @Override
    public Set<T> visitJoinReference(JoinReference reference) {

        if (referenceType.equals(JoinReference.class)) {
            references.add((T) reference);
        }

        JoinPath path = reference.getPath();

        int pathLimit = (mode == Mode.SAME_QUERY) ? 1 : path.getPathElements().size() - 1;

        for (int idx = 0; idx < pathLimit; idx++) {
            Path.PathElement pathElement = path.getPathElements().get(idx);

            String fieldName = pathElement.getFieldName();
            Type<?> parentClass = pathElement.getType();

            SQLTable table = metaDataStore.getTable(parentClass);
            SQLJoin join = table.getJoin(fieldName);

            if (visitedJoins.contains(join)) {
                continue;
            }
            visitedJoins.add(join);

            parser.parse(table, join.getJoinExpression()).stream().forEach(
                    ref -> ref.accept(this));
        }

        if (mode != Mode.SAME_QUERY) {
            return reference.getReference().accept(this);
        }

        return references;
    }

    @Override
    public Set<T> visitColumnArgReference(ColumnArgReference reference) {
        if (referenceType.equals(ColumnArgReference.class)) {
            references.add((T) reference);
        }

        return references;
    }

    @Override
    public Set<T> visitTableArgReference(TableArgReference reference) {
        if (referenceType.equals(TableArgReference.class)) {
            references.add((T) reference);
        }

        return references;
    }
}
