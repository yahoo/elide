/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.dimensions;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;

/**
 * A nested view for testing.
 */
@Data
@Include(rootLevel = false)
@FromTable(name = "countries")
public class CountryViewNested implements Serializable {
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
