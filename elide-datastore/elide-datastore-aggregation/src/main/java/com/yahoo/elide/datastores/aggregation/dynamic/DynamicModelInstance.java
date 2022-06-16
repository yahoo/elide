/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dynamic;

import com.yahoo.elide.core.type.Dynamic;
import com.yahoo.elide.core.type.ParameterizedModel;
import com.yahoo.elide.core.type.Type;

import java.io.Serializable;

/**
 * Base model instance returned by AggregationStore for dynamic types.
 */
public class DynamicModelInstance extends ParameterizedModel implements Dynamic, Serializable {
    private static final long serialVersionUID = -374837200186480683L;

    protected TableType tableType;

    public DynamicModelInstance(TableType type) {
        this.tableType = type;
    }

    @Override
    public Type getType() {
        return tableType;
    }
}
