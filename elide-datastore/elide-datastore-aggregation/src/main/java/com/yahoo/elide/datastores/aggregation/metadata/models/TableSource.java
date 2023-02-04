/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.google.common.base.Preconditions;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Pointer to a table column where a particular argument/column has its values defined.
 */
@Include(rootLevel = false, name = "tableSource")
@Data
@ToString
public class TableSource {
    @Id
    private String id;

    @OneToOne
    private Column valueSource;

    @ManyToMany
    Set<Column> suggestionColumns;

    public TableSource(Column valueSource, Set<Column> suggestionColumns) {
        this.id = valueSource.getId();
        this.valueSource = valueSource;
        this.suggestionColumns = suggestionColumns;
    }

    /**
     * Converts from a table source annotation to a table source metadata model.
     * @param sourceDefinition The source annotation.
     * @param apiVersion The api version.
     * @param metaDataStore The metadata store.
     * @return A new Table Source metadata model.
     */
    public static TableSource fromDefinition(
            com.yahoo.elide.datastores.aggregation.annotation.TableSource sourceDefinition,
            String apiVersion,
            MetaDataStore metaDataStore
    ) {
        if (sourceDefinition == null || sourceDefinition.table() == null || sourceDefinition.table().isEmpty()) {
            return null;
        }

        String sourceModelName = com.yahoo.elide.modelconfig.model.Table.getModelName(
                sourceDefinition.table(), sourceDefinition.namespace());

        com.yahoo.elide.datastores.aggregation.metadata.models.Table sourceTable =
                metaDataStore.getTable(sourceModelName, apiVersion);

        Preconditions.checkNotNull(sourceTable, "Unable to locate table Source table: "
                + sourceModelName);

        Column sourceColumn = sourceTable.getDimension(sourceDefinition.column());

        Preconditions.checkNotNull(sourceColumn, "Unable to locate table Source column: "
                + sourceDefinition.column());

        Set<Column> suggestionColumns = new HashSet<>();
        if (sourceDefinition.suggestionColumns().length > 0) {
            for (int idx = 0; idx < sourceDefinition.suggestionColumns().length; idx++) {
                String suggestionColumnName = sourceDefinition.suggestionColumns()[idx];
                Column suggestionColumn = sourceTable.getDimension(suggestionColumnName);

                Preconditions.checkNotNull(sourceTable, "Unable to locate table suggestion column: "
                        + suggestionColumnName);

                suggestionColumns.add(suggestionColumn);
            }
        }

        return new TableSource(sourceColumn, suggestionColumns);
    }
}
