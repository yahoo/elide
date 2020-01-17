/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.generateColumnReference;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * SQLTimeDimension are time dimension columns with extra physical information.
 */
public class SQLTimeDimension extends TimeDimension implements SQLColumn {
    @Getter
    private final String reference;

    @Getter
    private final List<JoinPath> joinPaths = new ArrayList<>();

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
}
