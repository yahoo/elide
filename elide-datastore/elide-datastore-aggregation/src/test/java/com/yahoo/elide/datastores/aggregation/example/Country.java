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

import java.util.Objects;
import java.util.StringJoiner;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Entity
@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.SMALL)
public class Country {

    private String id;

    private String isoCode;

    private String name;

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

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final Country country = (Country) other;
        return getId().equals(country.getId())
                && getIsoCode().equals(country.getIsoCode())
                && getName().equals(country.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getIsoCode(), getName());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Country.class.getSimpleName() + "[", "]")
                .add("id='" + getId() + "'")
                .add("isoCode='" + getIsoCode() + "'")
                .add("name='" + getName() + "'")
                .toString();
    }
}
