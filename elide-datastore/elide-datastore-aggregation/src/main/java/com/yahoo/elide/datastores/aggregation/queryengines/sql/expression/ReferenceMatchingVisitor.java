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
import java.util.function.Predicate;

/**
 * Traverses every reference expression in the reference tree and returns true if at least one reference matches
 * the provided matcher/predicate.  This visitor descends into joins.
 */
public class ReferenceMatchingVisitor implements ReferenceVisitor<Boolean> {

    private Predicate<String> matcher;
    private MetaDataStore store;
    private ExpressionParser parser;
    private Set<SQLJoin> visitedJoins;

    public ReferenceMatchingVisitor(Predicate<String> matcher, MetaDataStore store) {
        this.store = store;
        this.matcher = matcher;
        parser = new ExpressionParser(store);
        visitedJoins = new HashSet<>();
    }

    @Override
    public Boolean visitPhysicalReference(PhysicalReference reference) {
        return matcher.test(reference.getName());
    }

    @Override
    public Boolean visitLogicalReference(LogicalReference reference) {
        boolean matches = reference.getReferences().stream()
                .anyMatch(child -> child.accept(this));

        return matches | matcher.test(reference.getColumn().getExpression());
    }

    @Override
    public Boolean visitJoinReference(JoinReference reference) {
        JoinPath path = reference.getPath();

        for (Path.PathElement pathElement : path.getPathElements()) {

            //The last element is not a join but a reference to the joined field.
            if (pathElement.equals(path.lastElement().get())) {
                continue;
            }

            String fieldName = pathElement.getFieldName();
            Type<?> parentClass = pathElement.getType();

            SQLTable table = store.getTable(parentClass);
            SQLJoin join = table.getJoin(fieldName);

            if (visitedJoins.contains(join)) {
                continue;
            }
            visitedJoins.add(join);

            if (matcher.test(join.getJoinExpression())) {
                return true;
            }

            boolean matches = parser.parse(table, join.getJoinExpression()).stream().anyMatch(
                    ref -> ref.accept(this));

            if (matches) {
                return true;
            }
        }

        return reference.getReference().accept(this);
    }
}
