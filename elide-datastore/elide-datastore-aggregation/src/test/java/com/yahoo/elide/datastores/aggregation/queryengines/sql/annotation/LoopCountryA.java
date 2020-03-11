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

import org.hibernate.annotations.Formula;

import lombok.Data;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Data
@Entity
@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.SMALL)
public class LoopCountryA {
    @Setter
    private String id;

    @Setter
    private LoopCountryB countryB;

    @Setter
    private boolean inUsa;

    @Id
    public String getId() {
        return id;
    }

    @ManyToOne
    @JoinColumn(name = "id")
    public LoopCountryB getCountryB() {
        return countryB;
    }

    @DimensionFormula("CASE WHEN {{countryB.inUsa}} = 'United States' THEN true ELSE false END")
    @Formula("CASE WHEN name = 'United States' THEN true ELSE false END")
    public boolean isInUsa() {
        return inUsa;
    }
}
