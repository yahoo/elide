/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;
import com.yahoo.elide.datastores.aggregation.metric.Sum;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Entity
@Include(rootLevel = true)
public class VideoGame {

    @Id
    private Long id;

    @Column(name = "rounds")
    @MetricAggregation(aggregations = {Sum.class})
    Long sessions;

    @MetricAggregation(aggregations = {Sum.class})
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
