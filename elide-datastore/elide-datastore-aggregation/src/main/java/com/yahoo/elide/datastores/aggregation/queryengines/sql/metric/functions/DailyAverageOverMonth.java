package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.templates.SQLQueryTemplate;
import com.yahoo.elide.datastores.aggregation.time.RequestTimeDimension;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;
import com.yahoo.elide.request.Argument;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DailyAverageOverMonth extends SQLMetricFunction {
    public DailyAverageOverMonth() {
        super(
                "dailyAvgOverMonth",
                "daily average over month",
                "daily average of a metric over months",
                Collections.emptySet());
    }

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    Metric metric,
                                    String alias,
                                    List<Dimension> dimensions,
                                    RequestTimeDimension timeDimension) {
        try {
            SQLQueryTemplate subQuery = SqlSum.class.newInstance().resolve(
                    arguments,
                    metric,
                    alias,
                    dimensions,
                    new RequestTimeDimension(timeDimension.getTimeDimension(), TimeGrain.DAY));

            return SqlAvg.class.newInstance().resolve(
                    arguments,
                    subQuery.getMetrics().get(0),
                    alias,
                    subQuery.withTimeGrain(TimeGrain.MONTH));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InternalServerErrorException("Can't construct subquery template for " + getName() + ".");
        }
    }
}
