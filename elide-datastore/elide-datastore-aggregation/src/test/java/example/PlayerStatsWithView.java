/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import example.dimensions.Country;
import example.dimensions.CountryView;
import example.dimensions.SubCountry;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Include
@EqualsAndHashCode
@ToString
@FromTable(name = "playerStats")
public class PlayerStatsWithView implements Serializable {

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

    private CountryView countryView;

    @Setter
    private String countryViewIsoCode;

    @Setter
    private String countryViewViewIsoCode;

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

    private Date recordedDate;

    @Id
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @MetricFormula("MAX({{$highScore}})")
    @ColumnMeta(description = "very awesome score")
    public long getHighScore() {
        return highScore;
    }

    public void setHighScore(final long highScore) {
        this.highScore = highScore;
    }

    @MetricFormula("MIN({{$lowScore}})")
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

    @Join("{{$sub_country_id}} = {{subCountry.$id}}")
    public SubCountry getSubCountry() {
        return subCountry;
    }

    public void setSubCountry(final SubCountry subCountry) {
        this.subCountry = subCountry;
    }

    @Join("{{$player_id}} = {{player.$id}}")
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(final Player player) {
        this.player = player;
    }

    /**
     * <b>DO NOT put {@link Cardinality} annotation on this field</b>.
     *
     * @return the date of the player session.
     */
    @Temporal(grains = { @TimeGrainDefinition(grain = TimeGrain.DAY, expression = "")}, timeZone = "UTC")
    public Date getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(final Date recordedDate) {
        this.recordedDate = recordedDate;
    }

    @DimensionFormula("{{country.isoCode}}")
    public String getCountryIsoCode() {
        return countryIsoCode;
    }

    public void setCountryIsoCode(String isoCode) {
        this.countryIsoCode = isoCode;
    }


    @DimensionFormula("{{subCountry.isoCode}}")
    @Column(updatable = false, insertable = false) // subselect field should be read-only
    public String getSubCountryIsoCode() {
        return subCountryIsoCode;
    }

    public void setSubCountryIsoCode(String isoCode) {
        this.subCountryIsoCode = isoCode;
    }

    @Join("{{$country_id}} = {{countryView.$id}}")
    public CountryView getCountryView() {
        return countryView;
    }

    @DimensionFormula("{{countryView.$iso_code}}")
    public String getCountryViewIsoCode() {
        return countryViewIsoCode;
    }

    @DimensionFormula("{{countryView.nestedView.isoCode}}")
    public String getCountryViewViewIsoCode() {
        return countryViewViewIsoCode;
    }
}
