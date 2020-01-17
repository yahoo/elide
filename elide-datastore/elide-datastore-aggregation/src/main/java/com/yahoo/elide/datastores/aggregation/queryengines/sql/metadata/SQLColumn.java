/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.generateColumnReference;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * SQLColumn contains meta data about underlying physical table.
 */
public class SQLColumn extends Column {
    @Getter
    private final String reference;

    @Getter
    private final List<Path> joinPaths = new ArrayList<>();

    protected SQLColumn(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

        Path path = new Path(
                Collections.singletonList(
                        new Path.PathElement(
                                tableClass,
                                dictionary.getParameterizedType(tableClass, fieldName),
                                fieldName)));

        Map<Path, String> resolvedReferences = new HashMap<>();
        this.reference = generateColumnReference(path, new LinkedHashSet<>(), resolvedReferences, dictionary);
        this.joinPaths.addAll(resolvedReferences.keySet());
    }
}
