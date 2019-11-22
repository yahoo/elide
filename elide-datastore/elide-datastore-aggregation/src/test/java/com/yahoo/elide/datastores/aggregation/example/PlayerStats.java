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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlMax;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlMin;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
/**
 * A root level entity for testing AggregationDataStore.
 */
@Entity
@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
@FromTable(name = "playerStats")
public class PlayerStats {

    public static final String DAY_FORMAT = "PARSEDATETIME(FORMATDATETIME(%s, 'yyyy-MM-dd'), 'yyyy-MM-dd')";
    public static final String MONTH_FORMAT = "PARSEDATETIME(FORMATDATETIME(%s, 'yyyy-MM-01'), 'yyyy-MM-dd')";

    /**
     * PK.
     */
    private String id;

    /**
     * A metric.
     */
    private long highScore;

    /**
     * A metric.
     */
    private long lowScore;

    /**
     * A degenerate dimension.
     */
    private String overallRating;

    /**
     * A table dimension.
     */
    private Country country;

    /**
     * A subselect dimension.
     */
    private SubCountry subCountry;

    /**
     * A dimension field joined to this table.
     */
    private String countryIsoCode;

    /**
     * A dimension field joined to this table.
     */
    private String subCountryIsoCode;

    /**
     * A table dimension.
     */
    private Player player;

    private Player player2;

    private Date recordedDate;

    @Id
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @MetricAggregation(function = SqlMax.class)
    @Meta(longName = "awesome score", description = "very awesome score")
    public long getHighScore() {
        return highScore;
    }

    public void setHighScore(final long highScore) {
        this.highScore = highScore;
    }

    @MetricAggregation(function = SqlMin.class)
    public long getLowScore() {
        return lowScore;
    }

    public void setLowScore(final long lowScore) {
        this.lowScore = lowScore;
    }

    @FriendlyName
    @Cardinality(size = CardinalitySize.MEDIUM)
    public String getOverallRating() {
        return overallRating;
    }

    public void setOverallRating(final String overallRating) {
        this.overallRating = overallRating;
    }

    @ManyToOne
    @JoinColumn(name = "country_id")
    public Country getCountry() {
        return country;
    }

    public void setCountry(final Country country) {
        this.country = country;
    }

    @ManyToOne
    @JoinColumn(name = "sub_country_id")
    public SubCountry getSubCountry() {
        return subCountry;
    }

    public void setSubCountry(final SubCountry subCountry) {
        this.subCountry = subCountry;
    }

    @ManyToOne
    @JoinColumn(name = "player_id")
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(final Player player) {
        this.player = player;
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

    public void setRecordedDate(final Date recordedDate) {
        this.recordedDate = recordedDate;
    }

    @JoinTo(path = "country.isoCode")
    public String getCountryIsoCode() {
        return countryIsoCode;
    }

    public void setCountryIsoCode(String isoCode) {
        this.countryIsoCode = isoCode;
    }


    @JoinTo(path = "subCountry.isoCode")
    @Column(updatable = false, insertable = false) // subselect field should be read-only
    public String getSubCountryIsoCode() {
        return subCountryIsoCode;
    }

    public void setSubCountryIsoCode(String isoCode) {
        this.subCountryIsoCode = isoCode;
    }

    @JoinColumn(name = "player2_id")
    @ManyToOne
    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    private String playerName;

    private String player2Name;

    @JoinTo(path = "player.name")
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    @JoinTo(path = "player2.name")
    public String getPlayer2Name() {
        return player2Name;
    }

    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }
}
