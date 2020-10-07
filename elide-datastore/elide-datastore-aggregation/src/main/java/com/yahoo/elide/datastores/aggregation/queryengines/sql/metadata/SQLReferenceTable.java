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
import lombok.Value;

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

    private final Map<QueryableId, Map<String, String>> resolvedReferences = new HashMap<>();
    private final Map<QueryableId, Map<String, Set<JoinPath>>> resolvedJoinPaths = new HashMap<>();

    @Value
    private class QueryableId {
        String name;
        String version;
    }

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
        QueryableId id = new QueryableId(queryable.getName(), queryable.getVersion());
        return resolvedReferences.get(id).get(fieldName);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved reference
     */
    public Set<JoinPath> getResolvedJoinPaths(Queryable queryable, String fieldName) {
        QueryableId id = new QueryableId(queryable.getName(), queryable.getVersion());
        return resolvedJoinPaths.get(id).get(fieldName);
    }

    /**
     * Resolve all references and joins for a table and store them in this reference table.
     *
     * @param queryable meta data table
     */
    private void resolveAndStoreAllReferencesAndJoins(Queryable queryable) {
        QueryableId id = new QueryableId(queryable.getName(), queryable.getVersion());
        if (!resolvedReferences.containsKey(id)) {
            resolvedReferences.put(id, new HashMap<>());
        }
        if (!resolvedJoinPaths.containsKey(id)) {
            resolvedJoinPaths.put(id, new HashMap<>());
        }

        FormulaValidator validator = new FormulaValidator(metaDataStore);
        SQLJoinVisitor joinVisitor = new SQLJoinVisitor(metaDataStore);

        queryable.getColumns().forEach(column -> {
            // validate that there is no reference loop
            validator.visitColumn(column);

            String fieldName = column.getName();

            resolvedReferences.get(id).put(
                    fieldName,
                    new SQLReferenceVisitor(metaDataStore, queryable.getAlias()).visitColumn(column));

            resolvedJoinPaths.get(id).put(fieldName, joinVisitor.visitColumn(column));
        });
    }
}
