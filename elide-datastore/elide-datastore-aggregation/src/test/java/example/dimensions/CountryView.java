/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.dimensions;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;

import jakarta.persistence.Column;
import lombok.Data;

import java.io.Serializable;

/**
 * A view version of table countries.
 */
@Data
@Include(rootLevel = false)
@FromTable(name = "countries")
public class CountryView implements Serializable {
    @Column(name = "id")
    private String countryId;

    private String isoCode;

    private String name;

    private CountryViewNested nestedView;

    @Join("{{$id}} = {{nestedView.$id}}")
    public CountryViewNested getNestedView() {
        return nestedView;
    }

    @DimensionFormula("{{nestedView.isoCode}}")
    private String nestedViewIsoCode;

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(final String countryId) {
        this.countryId = countryId;
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
