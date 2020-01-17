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
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlMax;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlMin;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
/**
 * A root level entity for testing AggregationDataStore.
 */
@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
@FromTable(name = "playerStats")
public class PlayerStats {

    public static final String DAY_FORMAT = "PARSEDATETIME(FORMATDATETIME(%s, 'yyyy-MM-dd'), 'yyyy-MM-dd')";
    public static final String MONTH_FORMAT = "PARSEDATETIME(FORMATDATETIME(%s, 'yyyy-MM-01'), 'yyyy-MM-dd')";

    // PK
    @Setter
    private String id;

    // A metric
    @Setter
    private long highScore;

    // A metric
    @Setter
    private long lowScore;

    // degenerated dimension
    @Setter
    private String overallRating;

    // time dimension
    @Setter
    private Date recordedDate;

    // relationship dimension
    @Setter
    private Country country;

    // degenerated dimension from relationship
    @Setter
    private String countryIsoCode;

    // relationship dimension using @Subselect
    @Setter
    private SubCountry subCountry;

    // degenerated dimension from relationship using @Subselect
    @Setter
    private String subCountryIsoCode;

    // degenerated dimension using view
    @Setter
    private String countryViewIsoCode;

    // relationship dimension
    @Setter
    private Player player;

    // relationship dimension to the same table
    @Setter
    private Player player2;

    // degenerated dimension from relationship
    @Setter
    private String playerName;

    // degenerated dimension from relationship
    @Setter
    private String player2Name;

    // degenerated dimension using sql expression
    @Setter
    private int playerLevel;

    // degenerated dimension using sql expression from relationship dimension
    @Setter
    private boolean inUsa;

    // degenerated dimension using sql expression on another sql expression from relationship dimension
    @Setter
    private String countryIsInUsa;

    @Id
    public String getId() {
        return id;
    }

    @MetricAggregation(function = SqlMax.class)
    @Meta(longName = "awesome score", description = "very awesome score")
    public long getHighScore() {
        return highScore;
    }

    @MetricAggregation(function = SqlMin.class)
    public long getLowScore() {
        return lowScore;
    }

    @FriendlyName
    @Cardinality(size = CardinalitySize.MEDIUM)
    public String getOverallRating() {
        return overallRating;
    }

    @ManyToOne
    @JoinColumn(name = "country_id")
    public Country getCountry() {
        return country;
    }

    @ManyToOne
    @JoinColumn(name = "sub_country_id")
    public SubCountry getSubCountry() {
        return subCountry;
    }

    @ManyToOne
    @JoinColumn(name = "player_id")
    public Player getPlayer() {
        return player;
    }

    /**
     * <b>DO NOT put {@link Cardinality} annotation on this field</b>. See
     *
     * @return the date of the player session.
     */
    @Temporal(grains = {
            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DAY_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = MONTH_FORMAT)
    }, timeZone = "UTC")
    public Date getRecordedDate() {
        return recordedDate;
    }

    @JoinTo(path = "country.isoCode")
    public String getCountryIsoCode() {
        return countryIsoCode;
    }

    @JoinTo(path = "subCountry.isoCode")
    @Column(updatable = false, insertable = false) // subselect field should be read-only
    public String getSubCountryIsoCode() {
        return subCountryIsoCode;
    }

    @JoinColumn(name = "player2_id")
    @ManyToOne
    public Player getPlayer2() {
        return player2;
    }

    @JoinTo(path = "player.name")
    public String getPlayerName() {
        return playerName;
    }

    @JoinTo(path = "player2.name")
    public String getPlayer2Name() {
        return player2Name;
    }

    @DimensionFormula(
            expression = "CASE WHEN {%1} = 'Good' THEN 1 ELSE 2 END",
            references = {"overallRating"})
    public int getPlayerLevel() {
        return playerLevel;
    }

    @JoinTo(path = "country.inUsa")
    public boolean isInUsa() {
        return inUsa;
    }

    @DimensionFormula(
            expression = "CASE WHEN {%1} THEN 'true' ELSE 'false' END",
            references = {"country.inUsa"})
    public String getCountryIsInUsa() {
        return countryIsInUsa;
    }
}
