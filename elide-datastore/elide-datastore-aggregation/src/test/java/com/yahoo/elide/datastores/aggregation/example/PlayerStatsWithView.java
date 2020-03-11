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
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
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
@EqualsAndHashCode
@ToString
@FromTable(name = "playerStats")
public class PlayerStatsWithView {

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

    @Join("%from.country_id = %join.id")
    public Country getCountry() {
        return country;
    }

    public void setCountry(final Country country) {
        this.country = country;
    }

    @Join("%from.sub_country_id = %join.id")
    public SubCountry getSubCountry() {
        return subCountry;
    }

    public void setSubCountry(final SubCountry subCountry) {
        this.subCountry = subCountry;
    }

    @Join("%from.player_id = %join.id")
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
    @Temporal(grains = { @TimeGrainDefinition(grain = TimeGrain.DAY, expression = "") }, timeZone = "UTC")
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

    @Join("%from.country_id = %join.id")
    public CountryView getCountryView() {
        return countryView;
    }

    @JoinTo(path = "countryView.isoCode")
    public String getCountryViewIsoCode() {
        return countryViewIsoCode;
    }

    @JoinTo(path = "countryView.nestedView.isoCode")
    public String getCountryViewViewIsoCode() {
        return countryViewViewIsoCode;
    }
}
