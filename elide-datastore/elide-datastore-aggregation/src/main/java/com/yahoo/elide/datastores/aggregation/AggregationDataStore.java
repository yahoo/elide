package com.yahoo.elide.datastores.aggregation;


import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.engine.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.metric.AggregatedMetric;
import com.yahoo.elide.datastores.aggregation.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.metric.Metric;
import com.yahoo.elide.datastores.aggregation.metric.Sum;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import sun.awt.image.ImageWatched;

import java.sql.Time;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class AggregationDataStore implements DataStore {

    private QueryEngine queryEngine;

    public AggregationDataStore(QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
    }

    public AggregationDataStore() {

    }

    @Override
    public abstract void populateEntityDictionary(EntityDictionary dictionary);

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine);
    }

    public static Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
        SQLSchema schema = new SQLSchema(entityProjection.getType(), scope.getDictionary());
        AggregationDataStoreHelper agHelper = new AggregationDataStoreHelper(schema, entityProjection);
        FilterExpression filterExpression = entityProjection.getFilterExpression();
        FilterExpression whereFilter = agHelper.getWhereFilter(filterExpression);
        FilterExpression havingFilter = agHelper.getHavingFilter(filterExpression);
        Sorting sorting = entityProjection.getSorting();
        Pagination pagination = entityProjection.getPagination();
        Set<Dimension> dimensions = new LinkedHashSet<>();
        Set<TimeDimension> timeDimensions = new LinkedHashSet<>();
        agHelper.populateDimensionList(dimensions, timeDimensions);
        Set<Metric> metrics = new LinkedHashSet<>();
        agHelper.populateMetricList(metrics);
        Map<Metric, ? extends Class<? extends Aggregation>> metricClassMap = agHelper.getMetricMap(metrics);

        return Query.builder()
                    .schema(schema)
                    .metrics(metricClassMap)
                    .groupDimensions(dimensions)
                    .timeDimensions(timeDimensions)
                    .whereFilter(whereFilter)
                    .havingFilter(havingFilter)
                    .sorting(sorting)
                    .pagination(pagination)
                    .scope(scope)
                    .build();
    }
}
