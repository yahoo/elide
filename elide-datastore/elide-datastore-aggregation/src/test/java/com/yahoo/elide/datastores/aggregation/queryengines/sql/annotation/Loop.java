/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
public class Loop {
    // PK
    @Setter
    private String id;

    // degenerated dimension using sql expression
    @Setter
    private int playerLevel;

    @DimensionFormula("CASE WHEN {{playerLevel}} = 'Good' THEN 1 ELSE 2 END")
    public int getPlayerLevel() {
        return playerLevel;
    }
}
