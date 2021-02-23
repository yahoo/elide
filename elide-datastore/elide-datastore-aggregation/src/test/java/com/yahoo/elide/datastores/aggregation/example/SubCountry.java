/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import org.hibernate.annotations.Subselect;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * A root level entity for testing AggregationDataStore with @Subselect annotation.
 */
@Data
@Entity
@Include
@Subselect(value = "select * from countries")
public class SubCountry {

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

    @Column(name = "iso_code")
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
}
