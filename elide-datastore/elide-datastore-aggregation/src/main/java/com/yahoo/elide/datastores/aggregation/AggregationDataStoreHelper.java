package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.metric.Metric;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AggregationDataStoreHelper {

    private Schema schema;
    private EntityProjection entityProjection;

    public AggregationDataStoreHelper(Schema schema, EntityProjection entityProjection) {
        this.schema = schema;
        this.entityProjection = entityProjection;
    }

    public FilterExpression getWhereFilter(FilterExpression filterExpression) {
        FilterExpression whereFilter;
        try {
            whereFilter = filterExpression.accept(new SplitFilterExpressionVisitor(schema)).getWhereExpression();
        } catch (NullPointerException npe) {
            whereFilter = null;
        }
        return whereFilter;
    }

    public FilterExpression getHavingFilter(FilterExpression filterExpression) {
        FilterExpression havingFilter;
        try {
            havingFilter = filterExpression.accept(new SplitFilterExpressionVisitor(schema)).getHavingExpression();
        } catch (NullPointerException npe) {
            havingFilter = null;
        }
        return havingFilter;
    }


    public void populateDimensionList(Set<Dimension> dimensions, Set<TimeDimension> timeDimensions) {
        Set<String> dimensionNames = getDimensionNames();
        Set<String> metricNames = getMetricNames(); // time dimensions are under attribute in entity projection
        for (Dimension dimension : schema.getDimensions()) {
            if (dimension instanceof TimeDimension) {
                if(metricNames.contains(dimension.getName()))
                timeDimensions.add((TimeDimension)dimension);
            }
            else if (dimensionNames.contains(dimension.getName()) || metricNames.contains(dimension.getName())) {
                dimensions.add(dimension);
            }
        }
    }

    public void populateMetricList(Set<Metric> metrics) {
        Set<String> metricNames = getMetricNames();
        for (Metric metric : schema.getMetrics()) {
            if (metricNames.contains(metric.getName())) {
                metrics.add(metric);
            }
        }
    }

    public Map<Metric, Class<? extends Aggregation>> getMetricMap(Set<Metric> metrics) {
        Map<Metric, Class<? extends Aggregation>> metricMap = new LinkedHashMap<>();
        for (Metric metric : metrics) {
            metricMap.put(metric, metric.getAggregations().get(0));
        }
        return metricMap;
    }

    private Set<String> getMetricNames() {
        Set<String> metricNames = new LinkedHashSet<>();
        for (Attribute attribute : entityProjection.getAttributes()) {
            metricNames.add(attribute.getName());
        }
        return metricNames;
    }

    private Set<String> getDimensionNames() {
        Set<String> dimensionNames = new LinkedHashSet<>();
        for (Relationship relationship : entityProjection.getRelationships()) {
            dimensionNames.add(relationship.getName());
        }
        return dimensionNames;
    }
}
