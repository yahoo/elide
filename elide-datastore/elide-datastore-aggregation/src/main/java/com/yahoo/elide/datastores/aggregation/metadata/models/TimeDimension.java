/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Entity;

/**
 * TimeDimension is a dimension that represents time value.
 * This type of dimension can be used to support more specific aggregation logic e.g. DAILY/MONTHLY aggregation
 */
@EqualsAndHashCode(callSuper = true)
@Include(rootLevel = true, type = "timeDimension")
@Entity
@Data
public class TimeDimension extends Dimension {
    Set<TimeGrain> supportedGrains;

    public TimeDimension(Class<?> tableClass, String fieldName, AggregationDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

        Temporal temporal = dictionary.getAttributeOrRelationAnnotation(tableClass, Temporal.class, fieldName);

        this.supportedGrains = Arrays.stream(temporal.grains())
                .map(TimeGrainDefinition::grain)
                .collect(Collectors.toSet());
    }
}
