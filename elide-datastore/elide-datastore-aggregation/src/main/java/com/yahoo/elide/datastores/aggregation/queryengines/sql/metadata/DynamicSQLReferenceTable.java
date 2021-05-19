/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.Reference;
import com.google.common.collect.Sets;
import lombok.Getter;

import java.util.List;

/**
 * Table stores all resolved physical reference and join paths of all columns for both static tables
 * and an active query.  This class avoids copying the static reference table for each query.
 */
public class DynamicSQLReferenceTable extends SQLReferenceTable {

    //Stores the static table references
    @Getter
    private final SQLReferenceTable staticReferenceTable;

    public DynamicSQLReferenceTable(SQLReferenceTable staticReferenceTable, Queryable query) {
        super(staticReferenceTable.getMetaDataStore(), Sets.newHashSet(query));

        this.staticReferenceTable = staticReferenceTable;
    }

    @Override
    public List<Reference> getReferenceTree(Queryable queryable, String fieldName) {
        if (staticReferenceTable.referenceTree.containsKey(queryable)) {
            return staticReferenceTable.getReferenceTree(queryable, fieldName);
        }
        return referenceTree.get(queryable).get(fieldName);
    }
}
