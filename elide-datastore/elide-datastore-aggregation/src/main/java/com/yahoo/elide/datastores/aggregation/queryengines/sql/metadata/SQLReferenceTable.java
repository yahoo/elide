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
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Queryable;

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

    //Stores  MAP<table alias, MAP<fieldName, reference>>
    private final Map<String, Map<String, String>> resolvedReferences = new HashMap<>();

    //Stores  MAP<table alias, MAP<fieldName, join path>>
    private final Map<String, Map<String, Set<JoinPath>>> resolvedJoinPaths = new HashMap<>();

    public SQLReferenceTable(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.dictionary = this.metaDataStore.getMetadataDictionary();

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
        return resolvedReferences.get(queryable.getAlias()).get(fieldName);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved reference
     */
    public Set<JoinPath> getResolvedJoinPaths(Queryable queryable, String fieldName) {
        return resolvedJoinPaths.get(queryable.getAlias()).get(fieldName);
    }

    /**
     * Resolve all references and joins for a table and store them in this reference table.
     *
     * @param queryable meta data table
     */
    private void resolveAndStoreAllReferencesAndJoins(Queryable queryable) {
        String id = queryable.getAlias();
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
                    new SQLReferenceVisitor(metaDataStore, queryable.getAlias(fieldName)).visitColumn(column));

            resolvedJoinPaths.get(id).put(fieldName, joinVisitor.visitColumn(column));
        });
    }
}
