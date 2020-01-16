/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlSum;

import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Include(rootLevel = true)
@FromTable(name = "videoGames")
public class VideoGame {
    @Setter
    private Long id;

    @Setter
    Long sessions;

    @Setter
    Long timeSpent;

    @Setter
    private Float timeSpentPerSession;

    @Setter
    private Float timeSpentPerGame;

    @Setter
    private Player player;

    @Id
    public Long getId() {
        return id;
    }

    @Column(name = "game_rounds")
    @MetricAggregation(function = SqlSum.class)
    public Long getSessions() {
        return sessions;
    }

    @MetricAggregation(function = SqlSum.class)
    public Long getTimeSpent() {
        return timeSpent;
    }

    @MetricFormula(expression = "{%1} / (CASE WHEN {%2} = 0 THEN 1 ELSE {%2} END)", references = {"timeSpent", "sessions"})
    public Float getTimeSpentPerSession() {
        return timeSpentPerSession;
    }

    @MetricFormula(expression = "{%1} / 100", references = {"timeSpentPerSession"})
    public Float getTimeSpentPerGame() {
        return timeSpentPerGame;
    }

    @ManyToOne
    @JoinColumn(name = "player_id")
    public Player getPlayer() {
        return player;
    }
}
