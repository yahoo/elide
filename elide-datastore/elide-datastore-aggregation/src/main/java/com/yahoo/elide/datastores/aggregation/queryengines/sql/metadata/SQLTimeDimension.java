/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.generateColumnReference;
import static com.yahoo.elide.utils.TypeHelper.getFieldAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.LabelResolver;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
        Class<?> tableClass = dictionary.getEntityClass(table.getId());

        JoinPath path = new JoinPath(
                Collections.singletonList(
                        new Path.PathElement(
                                tableClass,
                                dictionary.getParameterizedType(tableClass, fieldName),
                                fieldName)));

        Map<JoinPath, String> resolvedReferences = new HashMap<>();
        this.reference = generateColumnReference(path, new LinkedHashSet<>(), resolvedReferences, dictionary);
        this.joinPaths.addAll(resolvedReferences.keySet());
    }

    @Override
    public Path getSourcePath(EntityDictionary metadataDictionary) {
        return joinPaths.isEmpty() ? super.getSourcePath(metadataDictionary) : joinPaths.get(0);
    }

    @Override
    public void resolveReference(MetaDataStore metaDataStore) {
        EntityDictionary dictionary = metaDataStore.getDictionary();
        String fieldName = getName();
        Class<?> tableClass = dictionary.getEntityClass(getTable().getId());

        this.reference = getLabelResolver().resolveLabel(
                new JoinPath(
                        Collections.singletonList(
                                new Path.PathElement(
                                        tableClass,
                                        dictionary.getParameterizedType(tableClass, fieldName),
                                        fieldName))),
                new LinkedHashSet<>(),
                new LinkedHashMap<>(),
                (joinPath, reference) -> {
                    if (joinPath != null) {
                        joinPaths.add(joinPath);
                        return getFieldAlias(joinPath, reference);
                    } else {
                        return reference;
                    }
                },
                metaDataStore);
    }

    @Override
    protected LabelResolver constructLabelResolver() {
        return sqlColumnLabelResolver();
    }
}
