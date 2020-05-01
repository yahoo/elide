/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import lombok.Value;

import java.util.Locale;
import javax.persistence.Id;

/**
 * Defines how to extract a time dimension for a specific grain from a table.
 */
@Include(type = "timeDimensionGrain")
@Value
public class TimeDimensionGrain {
    @Id String id;
    TimeGrain grain;
    String expression;

    public TimeDimensionGrain(String fieldName, TimeGrainDefinition definition) {
        this.id = fieldName + "." + definition.grain().name().toLowerCase(Locale.ENGLISH);
        this.grain = definition.grain();
        this.expression = definition.expression();
    }
}
