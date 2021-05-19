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
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extracts all the references of a particular type from the reference AST.
 * @param <T> The reference type to extract.
 */
public class ReferenceExtractor<T extends Reference> implements ReferenceVisitor<Set<T>> {

    private final Queryable limitedTo;
    private final Class<T> referenceType;
    private final Set<T> references;
    private MetaDataStore metaDataStore;
    private Set<SQLJoin> visitedJoins;
    private ExpressionParser parser;

    /**
     * Constructor.
     * @param referenceType The reference type to extract.
     * @param metaDataStore Metadata store.
     */
    public ReferenceExtractor(Class<T> referenceType, MetaDataStore metaDataStore) {
        this(referenceType, metaDataStore, null);
    }

    /**
     * Constructor.
     * @param referenceType The reference type to extract.
     * @param metaDataStore Metadata store.
     * @param limitedTo Only extract references that belong to the source table.  null means extract everything
     *                  regardless of source table.
     */
    public ReferenceExtractor(Class<T> referenceType, MetaDataStore metaDataStore, Queryable limitedTo) {
        this.limitedTo = limitedTo;
        this.referenceType = referenceType;
        this.references = new LinkedHashSet<>();
        this.metaDataStore = metaDataStore;
        this.parser = new ExpressionParser(metaDataStore);
        this.visitedJoins = new HashSet<>();
    }

    @Override
    public Set<T> visitPhysicalReference(PhysicalReference reference) {
        //This visitor is limited to references that belong to the given queryable.
        if (limitedTo != null && ! sameSourceTable(reference.getSource())) {
            return references;
        }

        if (referenceType.equals(PhysicalReference.class)) {
            references.add((T) reference);
        }

        return references;
    }

    @Override
    public Set<T> visitLogicalReference(LogicalReference reference) {
        //This visitor is limited to references that belong to the given queryable.
        if (limitedTo != null && ! sameSourceTable(reference.getSource())) {
            return references;
        }

        if (referenceType.equals(LogicalReference.class)) {
            references.add((T) reference);
        }

        reference.getReferences().stream()
                .map(ref -> ref.accept(this))
                .flatMap(Set::stream)
                .forEach(references::add);

        return references;
    }

    @Override
    public Set<T> visitJoinReference(JoinReference reference) {

        //This visitor is limited to references that belong to the given queryable.
        if (limitedTo != null && ! sameSourceTable(reference.getSource())) {
            return references;
        }

        if (referenceType.equals(JoinReference.class)) {
            references.add((T) reference);
        }

        JoinPath path = reference.getPath();

        for (int idx = 0; idx < path.getPathElements().size() - 1; idx++) {
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

        return reference.getReference().accept(this);
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

    private boolean sameSourceTable(Queryable referenceSource) {
        return limitedTo.getRoot().equals(referenceSource.getRoot());
    }
}
