/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.JoinType;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Include
@FromTable(name = "videoGames", dbConnectionName = "mycon")
public class VideoGame {
    @Setter
    private Long id;

    @Setter
    private Long sessions;

    @Setter
    private Long timeSpent;

    @Setter
    private Float timeSpentPerSession;

    @Setter
    private Float timeSpentPerGame;

    @Setter
    private Float normalizedHighScore;

    @Setter
    private Player player;

    @Setter
    private PlayerStats playerStats;

    @Setter
    private Player playerInnerJoin;

    @Setter
    private Player playerCrossJoin;

    @Setter
    private String playerName;

    @Setter
    private String playerNameInnerJoin;

    @Setter
    private String playerNameCrossJoin;

    @Id
    public Long getId() {
        return id;
    }

    @Column(name = "game_rounds")
    @MetricFormula("SUM({{game_rounds}})")
    public Long getSessions() {
        return sessions;
    }

    @MetricFormula("SUM({{timeSpent}})")
    public Long getTimeSpent() {
        return timeSpent;
    }

    @MetricFormula("({{timeSpent}} / (CASE WHEN SUM({{game_rounds}}) = 0 THEN 1 ELSE {{sessions}} END))")
    public Float getTimeSpentPerSession() {
        return timeSpentPerSession;
    }

    @MetricFormula("{{timeSpentPerSession}} / 100")
    public Float getTimeSpentPerGame() {
        return timeSpentPerGame;
    }


    @MetricFormula("{{playerStats.highScore}} / {{timeSpent}}")
    public Float getNormalizedHighScore() {
        return normalizedHighScore;
    }

    @Join(value = "{{player_id}} = {{player.id}}", type = JoinType.LEFT)
    public Player getPlayer() {
        return player;
    }

    @Join(value = "{{player_id}} = {{playerStats.id}}", type = JoinType.LEFT)
    public PlayerStats getPlayerStats() {
        return playerStats;
    }

    @Join(value = "{{player_id}} = {{playerInnerJoin.id}}", type = JoinType.INNER)
    public Player getPlayerInnerJoin() {
        return playerInnerJoin;
    }

    @Join(value = "", type = JoinType.CROSS)
    public Player getPlayerCrossJoin() {
        return playerCrossJoin;
    }

    @DimensionFormula("{{player.name}}")
    public String getPlayerName() {
        return playerName;
    }

    @DimensionFormula("{{playerInnerJoin.name}}")
    public String getPlayerNameInnerJoin() {
        return playerNameInnerJoin;
    }

    @DimensionFormula("{{playerCrossJoin.name}}")
    public String getPlayerNameCrossJoin() {
        return playerNameCrossJoin;
    }
}
