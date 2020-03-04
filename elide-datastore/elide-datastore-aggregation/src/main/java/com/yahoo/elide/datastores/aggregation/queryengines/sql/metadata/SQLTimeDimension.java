/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.LabelResolver;
import com.yahoo.elide.datastores.aggregation.metadata.LabelStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLTimeDimension are time dimension columns with extra physical information.
 */
public class SQLTimeDimension extends TimeDimension implements SQLColumn {
    @Getter
    private String reference;

    @Getter
    private List<JoinPath> joinPaths = new ArrayList<>();

    public SQLTimeDimension(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
    }

    @Override
    public Path getSourcePath(EntityDictionary metadataDictionary) {
        return joinPaths.isEmpty() ? super.getSourcePath(metadataDictionary) : joinPaths.get(0);
    }

    @Override
    public void resolveReference(LabelStore labelStore) {
        EntityDictionary dictionary = labelStore.getDictionary();
        String fieldName = getName();
        Class<?> tableClass = dictionary.getEntityClass(getTable().getId());

        this.reference = labelStore.resolveLabel(
                new JoinPath(
                        Collections.singletonList(
                                new Path.PathElement(
                                        tableClass,
                                        dictionary.getParameterizedType(tableClass, fieldName),
                                        fieldName))),
                "");
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
