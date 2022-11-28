/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static example.TimeGrainDefinitions.DATE_FORMAT;
import static example.TimeGrainDefinitions.MONTH_FORMAT;
import static example.TimeGrainDefinitions.QUARTER_FORMAT;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.type.ParameterizedModel;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.TableSource;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.custom.DailyAverageScorePerPeriodMaker;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.VersionQuery;
import com.yahoo.elide.datastores.aggregation.timegrains.Day;
import com.yahoo.elide.datastores.aggregation.timegrains.Time;
import com.fasterxml.jackson.annotation.JsonIgnore;
import example.dimensions.Country;
import example.dimensions.PlaceType;
import example.dimensions.SubCountry;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
/**
 * A root level entity for testing AggregationDataStore.
 */
@Include
@VersionQuery(sql = "SELECT COUNT(*) from playerStats")
@EqualsAndHashCode(callSuper = false)
@ToString
@FromTable(name = "playerStats")
@TableMeta(
        description = "Player Statistics",
        category = "Sports Category",
        tags = {"Game", "Statistics"},
        hints = {"AggregateBeforeJoin", "NoJoinBeforeAggregate"},
        size = CardinalitySize.LARGE
)
public class PlayerStats extends ParameterizedModel {

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
     * A metric.
     */
    private double dailyAverageScorePerPeriod;

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

    /**
     * A table dimension.
     */
    private PlayerRanking playerRanking;

    private Integer playerRank;

    private String playerName;

    private String player2Name;

    private Time recordedDate;

    private Day updatedDate;

    @Setter
    private int playerLevel;

    @Setter
    private String countryIsInUsa;

    private PlaceType placeType1;
    private PlaceType placeType2;

    @Id
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @MetricFormula("MAX({{$highScore}})")
    @ColumnMeta(description = "very awesome score", category = "Score Category")
    public long getHighScore() {
        return fetch("highScore", highScore);
    }

    public void setHighScore(final long highScore) {
        this.highScore = highScore;
    }

    @JsonIgnore
    @MetricFormula("MAX({{$highScore}})")
    @ColumnMeta(isHidden = true, description = "hidden metric", category = "Score Category")
    public long getHiddenHighScore() {
        return fetch("hiddenHighScore", highScore);
    }

    @MetricFormula("MIN({{$lowScore}})")
    @ColumnMeta(description = "very low score", category = "Score Category", tags = {"PRIVATE"})
    public long getLowScore() {
        return fetch("lowScore", lowScore);
    }

    public void setLowScore(final long lowScore) {
        this.lowScore = lowScore;
    }

    @MetricFormula(maker = DailyAverageScorePerPeriodMaker.class)
    public double getDailyAverageScorePerPeriod() {
        return fetch("dailyAverageScorePerPeriod", dailyAverageScorePerPeriod);
    }

    public void setDailyAverageScorePerPeriod(final double dailyAverageScorePerPeriod) {
        this.dailyAverageScorePerPeriod = dailyAverageScorePerPeriod;
    }

    @FriendlyName
    @ColumnMeta(values = {"Good", "OK", "Great", "Terrible"}, tags = {"PUBLIC"}, size = CardinalitySize.MEDIUM)
    public String getOverallRating() {
        return fetch("overallRating", overallRating);
    }

    public void setOverallRating(final String overallRating) {
        this.overallRating = overallRating;
    }

    @Join("{{$country_id}} = {{country.$id}}")
    public Country getCountry() {
        return fetch("country", country);
    }

    public void setCountry(final Country country) {
        this.country = country;
    }

    @DimensionFormula("{{country.nickName}}")
    @ColumnMeta(
            description = "SubCountry NickName",
            tableSource = @TableSource(table = "subCountry", column = "name", suggestionColumns = { "id", "isoCode" })
    )
    public String getCountryNickName() {
        return fetch("countryNickName", countryNickName);
    }

    public void setCountryNickName(String nickName) {
        this.countryNickName = nickName;
    }

    @DimensionFormula("{{country.unSeats}}")
    public int getCountryUnSeats() {
        return fetch ("countryUnSeats", countryUnSeats);
    }

    public void setCountryUnSeats(int seats) {
        this.countryUnSeats = seats;
    }

    @DimensionFormula("{{country.isoCode}}")
    @ColumnMeta(values = {"HKG", "USA"})
    public String getCountryIsoCode() {
        return fetch("countryIsoCode", countryIsoCode);
    }

    public void setCountryIsoCode(String isoCode) {
        this.countryIsoCode = isoCode;
    }

    @Join("{{$sub_country_id}} = {{subCountry.$id}}")
    public SubCountry getSubCountry() {
        return fetch("subCountry", subCountry);
    }

    public void setSubCountry(final SubCountry subCountry) {
        this.subCountry = subCountry;
    }

    @DimensionFormula("{{subCountry.isoCode}}")
    @Column(updatable = false, insertable = false) // subselect field should be read-only
    public String getSubCountryIsoCode() {
        return fetch("subCountryIsoCode", subCountryIsoCode);
    }

    public void setSubCountryIsoCode(String isoCode) {
        this.subCountryIsoCode = isoCode;
    }

    @Join("{{$player_id}} = {{playerRanking.$id}}")
    public PlayerRanking getPlayerRanking() {
        return fetch("playerRanking", playerRanking);
    }

    public void setPlayerRanking(final PlayerRanking playerRanking) {
        this.playerRanking = playerRanking;
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

    @DimensionFormula("{{playerRanking.ranking}}")
    public Integer getPlayerRank() {
        return fetch("playerRank", playerRank);
    }

    public void setPlayerRank(Integer playerRank) {
        this.playerRank = playerRank;
    }

    @DimensionFormula("{{player.name}}")
    public String getPlayerName() {
        return fetch("playerName", playerName);
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    @DimensionFormula("{{player2.name}}")
    public String getPlayer2Name() {
        return fetch("player2Name", player2Name);
    }

    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }

    @DimensionFormula("CASE WHEN {{overallRating}} = 'Good' THEN 1 ELSE 2 END")
    public int getPlayerLevel() {
        return fetch("playerLevel", playerLevel);
    }

    /**
     * <b>DO NOT put {@link Cardinality} annotation on this field</b>. See
     *
     * @return the date of the player session.
     */
    @Temporal(grains = {
            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DATE_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = MONTH_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.QUARTER, expression = QUARTER_FORMAT)
    }, timeZone = "UTC")
    @DimensionFormula("{{$recordedDate}}")
    public Time getRecordedDate() {
        return fetch("recordedDate", recordedDate);
    }

    public void setRecordedDate(final Time recordedDate) {
        this.recordedDate = recordedDate;
    }

    /**
     * <b>DO NOT put {@link Cardinality} annotation on this field</b>. See
     *
     * @return the date of the player session.
     */
    @JsonIgnore
    @Temporal(grains = {
            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DATE_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = MONTH_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.QUARTER, expression = QUARTER_FORMAT)
    }, timeZone = "UTC")
    @ColumnMeta(isHidden = true)
    @DimensionFormula("{{$recordedDate}}")
    public Time getHiddenRecordedDate() {
        return fetch("hiddenRecordedDate", recordedDate);
    }

    /**
     * <b>DO NOT put {@link Cardinality} annotation on this field</b>. See
     *
     * @return the date of the player session.
     */
    @Temporal(grains = { @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DATE_FORMAT) }, timeZone = "UTC")
    public Day getUpdatedDate() {
        return fetch("updatedDate", updatedDate);
    }

    public void setUpdatedDate(final Day updatedDate) {
        this.updatedDate = updatedDate;
    }

    @DimensionFormula("CASE WHEN {{country.inUsa}} THEN 'true' ELSE 'false' END")
    public String getCountryIsInUsa() {
        return fetch("countryIsInUsa", countryIsInUsa);
    }

    @DimensionFormula("{{$place_type_ordinal}}")
    @Enumerated(EnumType.ORDINAL)
    public PlaceType getPlaceType1() {
        return placeType1;
    }

    public void setPlaceType1(PlaceType placeType) {
        this.placeType1 = placeType;
    }

    @DimensionFormula("{{$place_type_text}}")
    @Enumerated(EnumType.STRING)
    public PlaceType getPlaceType2() {
        return placeType2;
    }

    public void setPlaceType2(PlaceType placeType) {
        this.placeType2 = placeType;
    }
}
