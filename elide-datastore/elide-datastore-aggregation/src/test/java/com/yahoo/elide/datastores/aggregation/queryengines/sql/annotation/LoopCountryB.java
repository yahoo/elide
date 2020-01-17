/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;

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
public class LoopCountryB {
    @Setter
    private String id;

    @Setter
    private LoopCountryA countryA;

    @Setter
    private boolean inUsa;

    @Id
    public String getId() {
        return id;
    }

    @ManyToOne
    @JoinColumn(name = "id")
    public LoopCountryA getCountryA() {
        return countryA;
    }

    @DimensionFormula(
            expression = "CASE WHEN {%1} = 'United States' THEN true ELSE false END",
            references = {"countryA.inUsa"})
    @Formula("CASE WHEN name = 'United States' THEN true ELSE false END")
    public boolean isInUsa() {
        return inUsa;
    }
}
