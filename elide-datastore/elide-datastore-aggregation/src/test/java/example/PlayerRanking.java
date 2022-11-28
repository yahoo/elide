/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Entity
@Include
@Data
@TableMeta(size = CardinalitySize.MEDIUM)
@Table(name = "playerRanking")
public class PlayerRanking {

    @Id
    private long id;

    private Integer ranking;
}
