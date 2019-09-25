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
        Schema schema = new Schema(entityProjection.getType(), scope.getDictionary());
        AggregationDataStoreHelper agHelper = new AggregationDataStoreHelper(schema, entityProjection);
        FilterExpression filterExpression = entityProjection.getFilterExpression();
        Optional<FilterExpression> whereFilter = agHelper.getWhereFilter(filterExpression);
        Optional<FilterExpression> havingFilter = agHelper.getHavingFilter(filterExpression);
        Optional<Sorting> sorting = Optional.ofNullable(entityProjection.getSorting());
        Optional<Pagination> pagination = Optional.ofNullable(entityProjection.getPagination());
        Set<Dimension> dimensions = new LinkedHashSet<>();
        Set<TimeDimension> timeDimensions = new LinkedHashSet<>();
        agHelper.populateDimensionList(dimensions, timeDimensions);
        Set<Metric> metrics = new LinkedHashSet<>();
        agHelper.populateMetricList(metrics);

        return Query.builder()
                    .entityClass(entityProjection.getType())
                    .metrics(metrics)
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
