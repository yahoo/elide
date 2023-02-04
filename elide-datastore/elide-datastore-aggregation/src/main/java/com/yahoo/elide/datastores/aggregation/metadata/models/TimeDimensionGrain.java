/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;

import jakarta.persistence.Id;
import lombok.Value;

import java.util.Locale;

/**
 * Defines how to extract a time dimension for a specific grain from a table.
 */
@Include(rootLevel = false, name = "timeDimensionGrain")
@Value
public class TimeDimensionGrain {
    @Id private final String id;
    private final TimeGrain grain;
    private final String expression;
    private final String format;

    public TimeDimensionGrain(String fieldName, TimeGrainDefinition definition) {
        this.id = getId(fieldName, definition.grain());
        this.grain = definition.grain();
        this.expression = definition.expression();
        this.format = definition.grain().getFormat();
    }

    public TimeDimensionGrain(String fieldName, TimeGrain grain) {
        this.id = getId(fieldName, grain);
        this.grain = grain;
        this.expression = "{{$$column.expr}}";
        this.format = grain.getFormat();
    }

    private static String getId(String fieldName, TimeGrain grain) {
        return fieldName + "." + grain.name().toLowerCase(Locale.ENGLISH);
    }
}
