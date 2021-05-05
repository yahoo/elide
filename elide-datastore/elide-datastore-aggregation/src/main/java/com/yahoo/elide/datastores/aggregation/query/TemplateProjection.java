/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;

import org.apache.commons.lang3.tuple.Pair;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * Represents a projected dimension column as an alias in a query.
 */
@AllArgsConstructor
@Data
@Builder
public class TemplateProjection implements ColumnProjection {

    private final String name;
    private final String expression;
    private final Map<String, Argument> arguments;

    @Override
    public ValueType getValueType() {
        return null;
    }

    @Override
    public ColumnType getColumnType() {
        return null;
    }

    @Override
    public Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source, MetaDataStore metaDataStore,
                    boolean joinInOuter) {
        return null;
    }

    @Override
    public <T extends ColumnProjection> T withProjected(boolean projected) {
        return null;
    }
}
