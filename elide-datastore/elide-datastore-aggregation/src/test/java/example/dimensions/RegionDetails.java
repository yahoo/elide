/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.dimensions;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A root level entity for testing AggregationDataStore.
 */
@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "region_details", dbConnectionName = "SalesDBConnection")
@ReadPermission(expression = "guest user")
@TableMeta(description = "RegionDetails", category = "", tags = {}, filterTemplate = "", size = CardinalitySize.SMALL)
@Include(name = "regionDetails")
public class RegionDetails {

    private String id;
    private String region;
    private PlaceType placeType;
    private PlaceType ordinalPlaceType;

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ReadPermission(expression = "guest user")
    @ColumnMeta(description = "region", category = "", values = {}, tags = {})
    @DimensionFormula("{{$region}}")
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Column(name = "type")
    public PlaceType getPlaceType() {
        return placeType;
    }

    public void setPlaceType(PlaceType placeType) {
        this.placeType = placeType;
    }

    @Column(name = "ordinal_type")
    public PlaceType getOrdinalPlaceType() {
        return ordinalPlaceType;
    }

    public void setOrdinalPlaceType(PlaceType placeType) {
        this.ordinalPlaceType = ordinalPlaceType;
    }
}
