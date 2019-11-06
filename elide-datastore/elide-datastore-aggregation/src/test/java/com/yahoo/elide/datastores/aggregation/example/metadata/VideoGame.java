/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example.metadata;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Metric;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;
import com.yahoo.elide.datastores.aggregation.metadata.metric.functions.Sum;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Entity
@Include(rootLevel = true)
@Table(name = "videoGames")
@FromTable(name = "videoGames")
public class VideoGame {

    @Id
    private Long id;

    @Column(name = "game_rounds")
    @Metric(function = Sum.class)
    Long sessions;

    @Metric(function = Sum.class)
    Long timeSpent;

    @MetricComputation(expression = "timeSpent / sessions")
    private Float timeSpentPerSession;

    @MetricComputation(expression = "timeSpentPerSession / 100")
    private Float timeSpentPerGame;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getSessions() {
        return sessions;
    }

    public void setSessions(final Long sessions) {
        this.sessions = sessions;
    }

    public Long getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(final Long timeSpent) {
        this.timeSpent = timeSpent;
    }

    public Float getTimeSpentPerSession() {
        return timeSpentPerSession;
    }

    public void setTimeSpentPerSession(final Float timeSpentPerSession) {
        this.timeSpentPerSession = timeSpentPerSession;
    }

    public Float getTimeSpentPerGame() {
        return timeSpentPerGame;
    }

    public void setTimeSpentPerGame(final Float timeSpentPerGame) {
        this.timeSpentPerGame = timeSpentPerGame;
    }
}
