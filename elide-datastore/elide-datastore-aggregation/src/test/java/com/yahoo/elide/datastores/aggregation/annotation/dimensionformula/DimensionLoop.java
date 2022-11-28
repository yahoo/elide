/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation.dimensionformula;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;

import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Include
@EqualsAndHashCode
@ToString
public class DimensionLoop {
    // PK
    @Setter
    private String id;

    // degenerated dimension using sql expression
    @Setter
    private int playerLevel1;

    @Setter
    private int playerLevel2;

    @Id
    public String getId() {
        return id;
    }

    @DimensionFormula("CASE WHEN {{playerLevel2}} = 'Good' THEN 1 ELSE 2 END")
    public int getPlayerLevel1() {
        return playerLevel1;
    }

    @DimensionFormula("CASE WHEN {{playerLevel1}} = 'Good' THEN 1 ELSE 2 END")
    public int getPlayerLevel2() {
        return playerLevel2;
    }
}
