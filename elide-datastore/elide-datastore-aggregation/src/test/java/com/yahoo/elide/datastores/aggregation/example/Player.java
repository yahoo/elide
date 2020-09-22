/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Entity
@Include
@Table(name = "players")
@Cardinality(size = CardinalitySize.MEDIUM)
@Data
public class Player {

    @Id
    private long id;

    @FriendlyName
    private String name;
}
