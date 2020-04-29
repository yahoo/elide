/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.getClassAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.FormulaValidator;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
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

    private final Map<Class<?>, Map<String, String>> resolvedReferences = new HashMap<>();
    private final Map<Class<?>, Map<String, Set<JoinPath>>> resolvedJoinPaths = new HashMap<>();

    public SQLReferenceTable(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.dictionary = this.metaDataStore.getDictionary();

        metaDataStore.getMetaData(Table.class).forEach(this::resolveAndStoreAllReferencesAndJoins);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param table table class
     * @param fieldName field name
     * @return resolved reference
     */
    public String getResolvedReference(Table table, String fieldName) {
        return resolvedReferences.get(dictionary.getEntityClass(table.getName(), table.getVersion())).get(fieldName);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param table table class
     * @param fieldName field name
     * @return resolved reference
     */
    public Set<JoinPath> getResolvedJoinPaths(Table table, String fieldName) {
        return resolvedJoinPaths.get(dictionary.getEntityClass(table.getName(), table.getVersion())).get(fieldName);
    }

    /**
     * Resolve all references and joins for a table and store them in this reference table.
     *
     * @param table meta data table
     */
    private void resolveAndStoreAllReferencesAndJoins(Table table) {
        Class<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());
        if (!resolvedReferences.containsKey(tableClass)) {
            resolvedReferences.put(tableClass, new HashMap<>());
        }
        if (!resolvedJoinPaths.containsKey(tableClass)) {
            resolvedJoinPaths.put(tableClass, new HashMap<>());
        }

        FormulaValidator validator = new FormulaValidator(metaDataStore);
        SQLJoinVisitor joinVisitor = new SQLJoinVisitor(metaDataStore);

        table.getColumns().forEach(column -> {
            // validate that there is no reference loop
            validator.visitColumn(column);

            String fieldName = column.getName();

            resolvedReferences.get(tableClass).put(
                    fieldName,
                    new SQLReferenceVisitor(metaDataStore, getClassAlias(tableClass)).visitColumn(column));

            resolvedJoinPaths.get(tableClass).put(fieldName, joinVisitor.visitColumn(column));
        });
    }
}
