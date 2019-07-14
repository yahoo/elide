/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.metric.Max;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Entity
@Include(rootLevel = true)
@Data
@FromSubquery(sql = "SELECT stats.highScore, stats.player_id, c.name FROM playerStats AS stats LEFT JOIN country AS c ON stats.country_id = c.id WHERE stats.overallRating = 'Great'")
public class PlayerStatsView {

    /**
     * PK.
     */
    @Id
    private String id;

    /**
     * A metric.
     */
    @MetricAggregation(aggregations = {Max.class})
    private long highScore;

    /**
     * A degenerate dimension.
     */
    private String countryName;

    @OneToOne
    @JoinColumn(name = "player_id")
    private Player player;
}
