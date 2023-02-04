/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import jakarta.persistence.ManyToMany;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * TimeDimension is a column that represents time value.
 * This type of column can be used to support time period (DAILY, MONTHLY, etc) aggregation.
 */
@EqualsAndHashCode(callSuper = true)
@Include(rootLevel = false, name = "timeDimension")
@Value
public class TimeDimension extends Column {
    @ManyToMany
    @ToString.Exclude
    LinkedHashSet<TimeDimensionGrain> supportedGrains;

    TimeZone timezone;

    public TimeDimension(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
        timezone = TimeZone.getTimeZone("UTC");

        Temporal temporal = dictionary.getAttributeOrRelationAnnotation(
                dictionary.getEntityClass(table.getName(), table.getVersion()),
                Temporal.class,
                fieldName);

        if (temporal.grains().length == 0) {
            this.supportedGrains = new LinkedHashSet<>(Arrays.asList(new TimeDimensionGrain(getId(), TimeGrain.DAY)));
        } else {
            this.supportedGrains = Arrays.stream(temporal.grains())
                    .map(grain -> new TimeDimensionGrain(getId(), grain))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public TimeDimensionGrain getDefaultGrain() {
        return supportedGrains.iterator().next();
    }
}
