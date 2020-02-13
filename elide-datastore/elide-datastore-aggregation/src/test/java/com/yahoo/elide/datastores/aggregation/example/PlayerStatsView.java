/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlMax;

import lombok.Data;

import javax.persistence.Id;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Include(rootLevel = true)
@Data
@FromSubquery(sql = "SELECT stats.highScore, stats.player_id, c.name as countryName FROM playerStats AS stats LEFT JOIN countries AS c ON stats.country_id = c.id WHERE stats.overallRating = 'Great'")
public class PlayerStatsView {

    /**
     * PK.
     */
    @Id
    private String id;

    /**
     * A metric.
     */
    @MetricAggregation(function = SqlMax.class)
    private long highScore;

    /**
     * A degenerate dimension.
     */
    private String countryName;

    @Join("%from.player_id = %join.id")
    private Player player;
}
