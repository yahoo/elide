/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Id;

/**
 * A root level entity for testing AggregationDataStore.
 */
@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "region_details", dbConnectionName = "SalesDBConnection")
@ReadPermission(expression = "Principal is guest user")
@TableMeta(description = "RegionDetails", category = "", tags = {}, filterTemplate = "", size = CardinalitySize.SMALL)
@Include(type = "regionDetails")
public class RegionDetails {

    private String id;
    private String region;

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ReadPermission(expression = "Principal is guest user")
    @ColumnMeta(description = "region", category = "", values = { "Virginia", "NewYork" }, tags = {}, tableSource = "")
    @DimensionFormula("{{region}}")
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
