/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import static example.TimeGrainDefinitions.DATE_FORMAT;
import static example.TimeGrainDefinitions.MONTH_FORMAT;
import static example.TimeGrainDefinitions.QUARTER_FORMAT;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.VersionQuery;
import com.yahoo.elide.datastores.aggregation.timegrains.Day;
import example.dimensions.Country;

import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Include
@VersionQuery(sql = "SELECT COUNT(*) from playerStats")
@EqualsAndHashCode(callSuper = false)
@ToString
@FromTable(name = "gameRevenue")
@TableMeta(
        description = "Game REvenue",
        hints = {"AggregateBeforeJoin"},
        size = CardinalitySize.LARGE
)
public class GameRevenue {
    @Id
    private int rowNumber;
    @Join("{{$country_id}} = {{country.$id}}")
    private Country country;

    @Join("{{$player_stats_id}} = {{playerStats.$id}}")
    private PlayerStats playerStats;

    @MetricFormula("MAX({{$revenue}})")
    private BigDecimal revenue;

    @DimensionFormula("{{country.isoCode}}")
    @ColumnMeta(values = {"HK", "US"})
    private String countryIsoCode;

    @DimensionFormula("{{$category}}")
    private String category;

    @Temporal(grains = {
            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DATE_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = MONTH_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.QUARTER, expression = QUARTER_FORMAT)
    }, timeZone = "UTC")
    @DimensionFormula("{{$saleDate}}")
    private Day saleDate;

    @Temporal(grains = {
            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DATE_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = MONTH_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.QUARTER, expression = QUARTER_FORMAT)
    }, timeZone = "UTC")
    @DimensionFormula("{{playerStats.recordedDate}}")
    private Day sessionDate;

    @Temporal(grains = {
            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DATE_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = MONTH_FORMAT),
            @TimeGrainDefinition(grain = TimeGrain.QUARTER, expression = QUARTER_FORMAT)
     }, timeZone = "UTC")
    @DimensionFormula("CASE WHEN {{sessionDate}} > {{saleDate}} THEN {{sessionDate}} ELSE {{saleDate}} END")
    private Day lastDate;

    @DimensionFormula("CASE WHEN {{countryIsoCode}} = 'US' THEN {{category}} ELSE 'UNKNONWN' END")
    private String countryCategory;
}
