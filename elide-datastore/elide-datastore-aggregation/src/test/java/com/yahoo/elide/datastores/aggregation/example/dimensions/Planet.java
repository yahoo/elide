/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example.dimensions;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import lombok.Data;

import javax.persistence.Id;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Data
@Include
@FromTable(name = "planets")
@TableMeta(isFact = false)
public class Planet {
    @Id
    private String id;

    private String name;
}
