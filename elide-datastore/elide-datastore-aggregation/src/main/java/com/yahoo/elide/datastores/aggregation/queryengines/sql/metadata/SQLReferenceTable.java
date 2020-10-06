/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.FormulaValidator;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Queryable;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Table stores all resolved physical reference and join paths of all columns.
 */
public class SQLReferenceTable {
    @Getter
    private final MetaDataStore metaDataStore;

    @Getter
    private final EntityDictionary dictionary;

    private final Map<Queryable, Map<String, String>> resolvedReferences = new HashMap<>();
    private final Map<Queryable, Map<String, Set<JoinPath>>> resolvedJoinPaths = new HashMap<>();

    public SQLReferenceTable(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.dictionary = this.metaDataStore.getDictionary();

        metaDataStore.getMetaData(Table.class).forEach(this::resolveAndStoreAllReferencesAndJoins);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved reference
     */
    public String getResolvedReference(Queryable queryable, String fieldName) {
        return resolvedReferences.get(queryable).get(fieldName);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved reference
     */
    public Set<JoinPath> getResolvedJoinPaths(Queryable queryable, String fieldName) {

        //TODO - The reference table either needs to lookup by the name of the table/view OR we need to
        //do a separate lookup outside the reference table.
        return resolvedJoinPaths.get(queryable).get(fieldName);
    }

    /**
     * Resolve all references and joins for a table and store them in this reference table.
     *
     * @param queryable meta data table
     */
    private void resolveAndStoreAllReferencesAndJoins(Queryable queryable) {
        if (!resolvedReferences.containsKey(queryable)) {
            resolvedReferences.put(queryable, new HashMap<>());
        }
        if (!resolvedJoinPaths.containsKey(queryable)) {
            resolvedJoinPaths.put(queryable, new HashMap<>());
        }

        FormulaValidator validator = new FormulaValidator(metaDataStore);
        SQLJoinVisitor joinVisitor = new SQLJoinVisitor(metaDataStore);

        queryable.getColumns().forEach(column -> {
            // validate that there is no reference loop
            validator.visitColumn(column);

            String fieldName = column.getName();

            resolvedReferences.get(queryable).put(
                    fieldName,
                    new SQLReferenceVisitor(metaDataStore, queryable.getAlias()).visitColumn(column));

            resolvedJoinPaths.get(queryable).put(fieldName, joinVisitor.visitColumn(column));
        });
    }
}
