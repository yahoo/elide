/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.SQLExpression;

import org.hibernate.annotations.Formula;

import lombok.Data;
import lombok.Setter;

import javax.persistence.Column;
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
    @Setter
    private String id;

    @Setter
    private String isoCode;

    @Setter
    private String name;

    @Setter
    private Continent continent;

    @Setter
    private boolean inUsa;

    @Id
    public String getId() {
        return id;
    }

    public String getIsoCode() {
        return isoCode;
    }

    @FriendlyName
    @Column(name = "name", insertable = false, updatable = false)
    public String getName() {
        return name;
    }

    @ManyToOne
    @JoinColumn(name = "continent_id")
    public Continent getContinent() {
        return continent;
    }

    @SQLExpression("CASE WHEN %reference = 'United States' THEN true ELSE false END")
    @Column(name = "name", insertable = false, updatable = false)
    @Formula("CASE WHEN name = 'United States' THEN true ELSE false END")
    public boolean isInUsa() {
        return inUsa;
    }
}
