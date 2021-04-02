package com.yahoo.elide.datastores.aggregation.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.VersionQuery;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import javax.persistence.Id;

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

    @MetricFormula("MAX({{$revenue}})")
    private BigDecimal revenue;

    @DimensionFormula("{{country.isoCode}}")
    @ColumnMeta(values = {"HK", "US"})
    private String countryIsoCode;

    @DimensionFormula("{{$category}}")
    private String category;
}
