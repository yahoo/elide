/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.VersionQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlMax;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions.SqlMin;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Id;
/**
 * A root level entity for testing AggregationDataStore.
 */
@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@VersionQuery(sql = "SELECT COUNT(*) from playerStats")
@EqualsAndHashCode
@ToString
@FromTable(name = "playerStats")
@Meta(description = "Player Statistics", category = "Sports Category")
public class PlayerStats {

    public static final String DAY_FORMAT = "PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')";
    public static final String MONTH_FORMAT = "PARSEDATETIME(FORMATDATETIME({{    }}, 'yyyy-MM-01'), 'yyyy-MM-dd')";

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

    private String countryNickName;

    private int countryUnSeats;

    /**
     * A subselect dimension.
     */
    private SubCountry subCountry;

    @Setter
    private String countryViewIsoCode;

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

    /**
     * A table dimension.
     */
    private Player player2;

    private String playerName;

    private String player2Name;

    private Date recordedDate;

    private Date updatedDate;

    @Setter
    private int playerLevel;

    @Setter
    private String countryIsInUsa;

    @Id
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @MetricAggregation(function = SqlMax.class)
    @Meta(description = "very awesome score", category = "Score Category")
    public long getHighScore() {
        return highScore;
    }

    public void setHighScore(final long highScore) {
        this.highScore = highScore;
    }

    @MetricAggregation(function = SqlMin.class)
    @Meta(description = "very low score", category = "Score Category")
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

    @Join("%from.country_id = %join.id")
    public Country getCountry() {
        return country;
    }

    public void setCountry(final Country country) {
        this.country = country;
    }

    @JoinTo(path = "country.nickName")
    public String getCountryNickName() {
        return countryNickName;
    }

    public void setCountryNickName(String nickName) {
        this.countryNickName = countryNickName;
    }

    @JoinTo(path = "country.unSeats")
    public int getCountryUnSeats() {
        return countryUnSeats;
    }

    public void setCountryUnSeats(int seats) {
        this.countryUnSeats = seats;
    }

    @JoinTo(path = "country.isoCode")
    public String getCountryIsoCode() {
        return countryIsoCode;
    }

    public void setCountryIsoCode(String isoCode) {
        this.countryIsoCode = isoCode;
    }

    @Join("%from.sub_country_id = %join.id")
    public SubCountry getSubCountry() {
        return subCountry;
    }

    public void setSubCountry(final SubCountry subCountry) {
        this.subCountry = subCountry;
    }

    @JoinTo(path = "subCountry.isoCode")
    @Column(updatable = false, insertable = false) // subselect field should be read-only
    public String getSubCountryIsoCode() {
        return subCountryIsoCode;
    }

    public void setSubCountryIsoCode(String isoCode) {
        this.subCountryIsoCode = isoCode;
    }

    @Join("%from.player_id = %join.id")
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(final Player player) {
        this.player = player;
    }

    @Join("%from.player2_id = %join.id")
    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

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

    @DimensionFormula("CASE WHEN {{overallRating}} = 'Good' THEN 1 ELSE 2 END")
    public int getPlayerLevel() {
        return playerLevel;
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

    /**
     * <b>DO NOT put {@link Cardinality} annotation on this field</b>. See
     *
     * @return the date of the player session.
     */
    @Temporal(grains = {
            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DAY_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = MONTH_FORMAT)
    }, timeZone = "UTC")
    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(final Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    @DimensionFormula("CASE WHEN {{country.inUsa}} THEN 'true' ELSE 'false' END")
    public String getCountryIsInUsa() {
        return countryIsInUsa;
    }
}
