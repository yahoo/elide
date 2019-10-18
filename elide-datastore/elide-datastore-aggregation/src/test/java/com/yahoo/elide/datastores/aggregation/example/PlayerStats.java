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
import com.yahoo.elide.datastores.aggregation.schema.dimension.EntityDimensionTest;
import com.yahoo.elide.datastores.aggregation.schema.metric.Max;
import com.yahoo.elide.datastores.aggregation.schema.metric.Min;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

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
     * A dimension field joined to this table.
     */
    private String countryIsoCode;

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

    @MetricAggregation(aggregations = {Max.class, Min.class})
    @Meta(longName = "awesome score", description = "very awesome score")
    public long getHighScore() {
        return highScore;
    }

    public void setHighScore(final long highScore) {
        this.highScore = highScore;
    }

    @MetricAggregation(aggregations = {Max.class, Min.class})
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

    @OneToOne
    @JoinColumn(name = "country_id")
    public Country getCountry() {
        return country;
    }

    public void setCountry(final Country country) {
        this.country = country;
    }

    @OneToOne
    @JoinColumn(name = "player_id")
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(final Player player) {
        this.player = player;
    }

    /**
     * <b>DO NOT put {@link Cardinality} annotation on this field</b>. See
     * {@link EntityDimensionTest#testCardinalityScan()}.
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
}
