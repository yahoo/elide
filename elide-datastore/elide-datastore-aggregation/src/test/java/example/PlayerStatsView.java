/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;

import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Include
@Data
@FromSubquery(sql = "SELECT stats.highScore, stats.player_id, c.name as countryName FROM playerStats AS stats "
                + "LEFT JOIN countries AS c ON stats.country_id = c.id "
                + "WHERE stats.overallRating = '{{$$table.args.rating}}' AND stats.highScore >= {{$$table.args.minScore}}")
@TableMeta(arguments = {
                @ArgumentDefinition(name = "rating", type = ValueType.TEXT),
                @ArgumentDefinition(name = "minScore", type = ValueType.INTEGER, defaultValue = "0")})
public class PlayerStatsView implements Serializable {

    /**
     * PK.
     */
    @Id
    private String id;

    /**
     * A metric.
     */
    @MetricFormula("MAX({{$highScore}})")
    private long highScore;

    /**
     * A degenerate dimension.
     */
    //TODO - @DimensionFormula(value = "$$column.args.format({{$countryName}})", arguments = {
    @DimensionFormula(value = "{{$countryName}}", arguments = {
            @ArgumentDefinition(name = "format", values = {"lower", "upper"})
    })
    private String countryName;

    @Join("{{$player_id}} = {{player.$id}}")
    private Player player;
}
