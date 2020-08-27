/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.TimeZone;
import javax.persistence.ManyToMany;

/**
 * TimeDimension is a dimension that represents time value.
 * This type of dimension can be used to support more specific aggregation logic e.g. DAILY/MONTHLY aggregation
 */
@EqualsAndHashCode(callSuper = true)
@Include(type = "timeDimension")
@Value
public class TimeDimension extends Dimension {
    @ManyToMany
    @ToString.Exclude
    TimeDimensionGrain supportedGrain;

    TimeZone timezone;

    public TimeDimension(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
        timezone = TimeZone.getTimeZone("UTC");

        Temporal temporal = dictionary.getAttributeOrRelationAnnotation(
                dictionary.getEntityClass(table.getName(), table.getVersion()),
                Temporal.class,
                fieldName);

        this.supportedGrain = new TimeDimensionGrain(getId(), temporal.grain());
    }
}
