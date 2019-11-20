/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

/**
 * TimeDimension is a dimension that represents time value.
 * This type of dimension can be used to support more specific aggregation logic e.g. DAILY/MONTHLY aggregation
 */
@EqualsAndHashCode(callSuper = true)
@Include(type = "timeDimension")
@Entity
@Data
public class TimeDimension extends Dimension {
    @ManyToMany
    Set<TimeDimensionGrain> supportedGrains;

    private TimeZone timezone;

    public TimeDimension(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

        Temporal temporal = dictionary.getAttributeOrRelationAnnotation(tableClass, Temporal.class, fieldName);

        this.supportedGrains = Arrays.stream(temporal.grains())
                .map(grain -> new TimeDimensionGrain(getId(), grain))
                .collect(Collectors.toSet());
    }
}
