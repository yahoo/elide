/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.google.common.collect.Sets;
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
        return resolvedReferences.get(queryable).get(fieldName);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved reference
     */
    @Override
    public Set<JoinPath> getResolvedJoinPaths(Queryable queryable, String fieldName) {
        if (staticReferenceTable.resolvedJoinPaths.containsKey(queryable)) {
            return staticReferenceTable.getResolvedJoinPaths(queryable, fieldName);
        }
        return resolvedJoinPaths.get(queryable).get(fieldName);
    }
}
