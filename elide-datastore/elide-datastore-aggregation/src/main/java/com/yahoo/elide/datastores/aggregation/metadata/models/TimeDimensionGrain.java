/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import lombok.Data;

import java.util.Locale;
import javax.persistence.Id;

/**
 * Defines how to extract a time dimension for a specific grain from a table.
 */
@Include(type = "timeDimensionGrain")
@Data
public class TimeDimensionGrain {
    @Id
    private String id;

    private TimeGrain grain;

    private String expression;

    public TimeDimensionGrain(String fieldName, TimeGrainDefinition definition) {
        this.id = fieldName + "." + definition.grain().name().toLowerCase(Locale.ENGLISH);
        this.grain = definition.grain();
        this.expression = definition.expression();
    }
}
