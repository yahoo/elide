/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.dimensions;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;

import jakarta.persistence.Id;
import lombok.Data;

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
