/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import javafx.util.Pair;

/**
 * Column with physical SQL information like reference and join to path.
 */
public interface SQLColumn {
    Table getTable();

    String getName();

    String getReference();

    JoinPath getJoinPath();

    default Pair<String, String> getSourceTableAndColumn(EntityDictionary metadataDictionary) {
        JoinPath joinPath = getJoinPath();
        if (joinPath == null) {
            return new Pair<>(getTable().getId(), getName());
        } else {
            Path.PathElement last = joinPath.lastElement().get();
            return new Pair<>(metadataDictionary.getJsonAliasFor(last.getType()), last.getFieldName());
        }
    }
}
