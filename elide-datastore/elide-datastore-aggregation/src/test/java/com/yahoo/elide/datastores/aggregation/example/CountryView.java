/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ToOne;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;

import lombok.Data;

import javax.persistence.Column;

/**
 * A view version of table countries.
 */
@Data
@Include
@FromTable(name = "countries")
public class CountryView {

    private String id;

    private String isoCode;

    private String name;

    @ToOne
    @JoinTo(
            joinClause = "%from.id = %join.id"
    )
    private CountryViewNested nestedView;

    @JoinTo(path = "nestedView.isoCode")
    private String nestedViewIsoCode;

    @ToOne
    @Column(name = "id")
    private Country nestedRelationship;

    @JoinTo(path = "nestedRelationship.isoCode")
    private String nestedRelationshipIsoCode;

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
}
