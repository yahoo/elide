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
import com.yahoo.elide.datastores.aggregation.query.Query;
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

    //Stores  MAP<Queryable, MAP<fieldName, reference>>
    private final Map<Queryable, Map<String, String>> resolvedReferences = new HashMap<>();

    //Stores  MAP<Queryable, MAP<fieldName, join path>>
    private final Map<Queryable, Map<String, Set<JoinPath>>> resolvedJoinPaths = new HashMap<>();

    public SQLReferenceTable(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.dictionary = this.metaDataStore.getMetadataDictionary();

        metaDataStore.getMetaData(Table.class)
                .stream()
                .map(SQLTable.class::cast)
                .forEach(this::resolveAndStoreAllReferencesAndJoins);
    }

    /**
     * Builds a new SQLReference table by starting from a base table and adding references from a particular query.
     * @param toCopy The base reference table.
     * @param query The query from which new references will be populated.
     */
    public SQLReferenceTable(SQLReferenceTable toCopy, Query query) {
        this.metaDataStore = toCopy.getMetaDataStore();
        this.dictionary = toCopy.getDictionary();
        this.resolvedJoinPaths.putAll(toCopy.resolvedJoinPaths);
        this.resolvedReferences.putAll(toCopy.resolvedReferences);

        Queryable next = query;

        while (next.isNested()) {
            resolveAndStoreAllReferencesAndJoins(next);

            next = next.getSource();
        }
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
        return resolvedJoinPaths.get(queryable).get(fieldName);
    }

    /**
     * Resolve all references and joins for a table and store them in this reference table.
     *
     * @param queryable meta data table
     */
    public void resolveAndStoreAllReferencesAndJoins(Queryable queryable) {

        //References and joins are stored by their source that produces them (rather than the query that asks for them).
        Queryable key = queryable.getSource();
        if (!resolvedReferences.containsKey(key)) {
            resolvedReferences.put(key, new HashMap<>());
        }
        if (!resolvedJoinPaths.containsKey(key)) {
            resolvedJoinPaths.put(key, new HashMap<>());
        }

        FormulaValidator validator = new FormulaValidator(metaDataStore);
        SQLJoinVisitor joinVisitor = new SQLJoinVisitor(metaDataStore);

        queryable.getColumnProjections().forEach(column -> {
            // validate that there is no reference loop
            validator.visitColumn(column);

            String fieldName = column.getName();

            resolvedReferences.get(key).put(
                    fieldName,
                    new SQLReferenceVisitor(metaDataStore, key.getAlias(fieldName)).visitColumn(column));

            resolvedJoinPaths.get(key).put(fieldName, joinVisitor.visitColumn(column));
        });
    }
}
