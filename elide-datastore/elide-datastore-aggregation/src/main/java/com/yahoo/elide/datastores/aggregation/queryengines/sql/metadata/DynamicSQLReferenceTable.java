/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

/**
 * Table stores all resolved physical reference and join paths of all columns for both static tables
 * and an active query.  This class avoids copying the static reference table for each query.
 */
public class DynamicSQLReferenceTable extends SQLReferenceTable {

    //Stores the static table references
    private final SQLReferenceTable staticReferenceTable;


    public DynamicSQLReferenceTable(SQLReferenceTable staticReferenceTable, Queryable query) {
        super(staticReferenceTable.getMetaDataStore(), Sets.newHashSet(query));

        this.staticReferenceTable = staticReferenceTable;
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved reference
     */
    @Override
    public String getResolvedReference(Queryable queryable, String fieldName) {
        if (staticReferenceTable.resolvedReferences.containsKey(queryable)) {
            return staticReferenceTable.getResolvedReference(queryable, fieldName);
        }

        String reference = resolvedReferences.get(queryable).get(fieldName);

        if (reference == null) {
            reference = staticReferenceTable.resolvedReferences.get(queryable.getRoot()).get(fieldName);
        }

        return reference;
    }

    /**
     * Get the resolved ON clause expression for a field from storage.
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved ON clause expression
     */
    @Override
    public Set<String> getResolvedJoinExpressions(Queryable queryable, String fieldName) {
        if (staticReferenceTable.resolvedJoinExpressions.containsKey(queryable)) {
            return staticReferenceTable.getResolvedJoinExpressions(queryable, fieldName);
        }

        Set<String> joinExpressions = resolvedJoinExpressions.get(queryable).get(fieldName);

        if (joinExpressions == null) {
            joinExpressions = staticReferenceTable.resolvedJoinExpressions.get(queryable.getRoot()).get(fieldName);
        }

        if (joinExpressions == null) {
            return new HashSet<>();
        }

        return joinExpressions;
    }

    @Override
    public Set<SQLColumnProjection> getResolvedJoinProjections(Queryable queryable, String fieldName) {
        if (staticReferenceTable.resolvedJoinProjections.containsKey(queryable)) {
            return staticReferenceTable.getResolvedJoinProjections(queryable, fieldName);
        }
        return super.getResolvedJoinProjections(queryable, fieldName);
    }
}
