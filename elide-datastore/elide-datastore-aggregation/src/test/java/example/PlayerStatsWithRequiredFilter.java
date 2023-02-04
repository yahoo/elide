/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static example.TimeGrainDefinitions.DATE_FORMAT;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.VersionQuery;
import example.dimensions.Country;
import example.dimensions.SubCountry;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Include(name = "playerStatsFiltered")
@VersionQuery(sql = "SELECT COUNT(*) from playerStats")
@EqualsAndHashCode
@ToString
@FromTable(name = "playerStats")
@TableMeta(
        description = "Player Statistics",
        category = "Sports Category",
        filterTemplate = PlayerStatsWithRequiredFilter.FILTER_TEMPLATE,
        size = CardinalitySize.LARGE
)
public class PlayerStatsWithRequiredFilter {
    public static final String FILTER_TEMPLATE = "recordedDate>={{start}};recordedDate<{{end}}";

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

    @MetricFormula("MAX(highScore)")
    @ColumnMeta(description = "very awesome score", category = "Score Category", filterTemplate = "recordedDate>{{recordedDate}}")
    public long getHighScore() {
        return highScore;
    }

    public void setHighScore(final long highScore) {
        this.highScore = highScore;
    }

    @MetricFormula(value = "{{$highScore}}")
    @ColumnMeta(description = "highScore with no aggregation")
    public long getHighScoreNoAgg() {
        return highScore;
    }
    public void setHighScoreNoAgg(final long highScore) {
        this.highScore = highScore;
    }

    @MetricFormula("MIN({{$lowScore}})")
    @ColumnMeta(description = "very low score", category = "Score Category")
    public long getLowScore() {
        return lowScore;
    }

    public void setLowScore(final long lowScore) {
        this.lowScore = lowScore;
    }

    @FriendlyName
    @ColumnMeta(size = CardinalitySize.MEDIUM)
    public String getOverallRating() {
        return overallRating;
    }

    public void setOverallRating(final String overallRating) {
        this.overallRating = overallRating;
    }

    @Join("{{$country_id}} = {{country.$id}}")
    public Country getCountry() {
        return country;
    }

    public void setCountry(final Country country) {
        this.country = country;
    }

    @DimensionFormula("{{country.nickName}}")
    public String getCountryNickName() {
        return countryNickName;
    }

    public void setCountryNickName(String nickName) {
        this.countryNickName = nickName;
    }

    @DimensionFormula("{{country.unSeats}}")
    public int getCountryUnSeats() {
        return countryUnSeats;
    }

    public void setCountryUnSeats(int seats) {
        this.countryUnSeats = seats;
    }

    @DimensionFormula("{{country.isoCode}}")
    public String getCountryIsoCode() {
        return countryIsoCode;
    }

    public void setCountryIsoCode(String isoCode) {
        this.countryIsoCode = isoCode;
    }

    @Join("{{$sub_country_id}} = {{subCountry.$id}}")
    public SubCountry getSubCountry() {
        return subCountry;
    }

    public void setSubCountry(final SubCountry subCountry) {
        this.subCountry = subCountry;
    }

    @DimensionFormula("{{subCountry.isoCode}}")
    @Column(updatable = false, insertable = false) // subselect field should be read-only
    public String getSubCountryIsoCode() {
        return subCountryIsoCode;
    }

    public void setSubCountryIsoCode(String isoCode) {
        this.subCountryIsoCode = isoCode;
    }

    @Join("{{$player_id}} = {{player.$id}}")
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(final Player player) {
        this.player = player;
    }

    @Join("{{$player2_id}} = {{player2.$id}}")
    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    @DimensionFormula("{{player.name}}")
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    @DimensionFormula("{{player2.name}}")
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
    @Temporal(grains = { @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DATE_FORMAT)}, timeZone = "UTC")
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
    @Temporal(grains = { @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DATE_FORMAT)}, timeZone = "UTC")
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
