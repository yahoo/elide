/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.utils.TypeHelper.getFieldAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.LabelResolver;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLDimension are dimension columns with extra physical information.
 */
public class SQLDimension extends Dimension implements SQLColumn {
    @Getter
    private String reference;

    @Getter
    private List<JoinPath> joinPaths = new ArrayList<>();

    public SQLDimension(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
    }

    @Override
    public Path getSourcePath(EntityDictionary metadataDictionary) {
        // As we are using DFS for resolving reference, the first resolved reference would be the deepest source
        return joinPaths.isEmpty() ? super.getSourcePath(metadataDictionary) : joinPaths.get(0);
    }

    @Override
    public void resolveReference(MetaDataStore metaDataStore) {
        EntityDictionary dictionary = metaDataStore.getDictionary();
        String fieldName = getName();
        Class<?> tableClass = dictionary.getEntityClass(getTable().getId());

        this.reference = metaDataStore.resolveLabel(
                new JoinPath(
                        Collections.singletonList(
                                new Path.PathElement(
                                        tableClass,
                                        dictionary.getParameterizedType(tableClass, fieldName),
                                        fieldName))),
                (joinPath, reference) -> {
                    if (joinPath != null) {
                        joinPaths.add(joinPath);
                        return getFieldAlias(joinPath, reference);
                    } else {
                        return reference;
                    }
                });
    }

    @Override
    protected LabelResolver constructLabelResolver(EntityDictionary dictionary) {
        return constructSQLColumnLabelResolver(dictionary);
    }

    @Override
    public Column getColumn() {
        return this;
    }
}
