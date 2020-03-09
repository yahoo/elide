/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;

import org.hibernate.annotations.Formula;

import lombok.Data;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Data
@Entity
@Include(rootLevel = true)
@Table(name = "countries")
@Cardinality(size = CardinalitySize.SMALL)
public class Country {

    private String id;

    private String isoCode;

    private String name;

    private Continent continent;

    @Setter
    private boolean inUsa;

    @Id
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(final String isoCode) {
        this.isoCode = isoCode;
    }

    @FriendlyName
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @ManyToOne
    @JoinColumn(name = "continent_id")
    public Continent getContinent() {
        return continent;
    }

    public void setContinent(Continent continent) {
        this.continent = continent;
    }

    @DimensionFormula("CASE WHEN {{name}} = 'United States' THEN true ELSE false END")
    @Formula("CASE WHEN name = 'United States' THEN true ELSE false END")
    public boolean isInUsa() {
        return inUsa;
    }
}
