package com.yahoo.elide.spring.models.aggregation;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Id;

@Include(rootLevel = true)
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
@FromTable(name = "stats")
public class Stats {

    /**
     * PK.
     */
    @Id
    private String id;

    /**
     * A metric.
     */
    private long measure;

    /**
     * A degenerate dimension.
     */
    private String dimension;
}
