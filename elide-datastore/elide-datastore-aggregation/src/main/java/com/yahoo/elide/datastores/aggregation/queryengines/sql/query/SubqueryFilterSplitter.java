/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.visitors.FilterExpressionNormalizationVisitor;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class SubqueryFilterSplitter
        implements FilterExpressionVisitor<Pair<FilterExpression, FilterExpression>> {

    private SQLReferenceTable lookupTable;
    private MetaDataStore metaDataStore;

    public SubqueryFilterSplitter(MetaDataStore metaDataStore, SQLReferenceTable lookupTable) {
        this.metaDataStore = metaDataStore;
        this.lookupTable = lookupTable;
    }

    public static Pair<FilterExpression, FilterExpression> splitFilter(
            SQLReferenceTable lookupTable,
            MetaDataStore metaDataStore,
            FilterExpression expression) {
        FilterExpressionNormalizationVisitor normalizer = new FilterExpressionNormalizationVisitor();
        FilterExpression normalizedExpression = expression.accept(normalizer);

        return normalizedExpression.accept(new SubqueryFilterSplitter(metaDataStore, lookupTable));
    }

    @Override
    public Pair<FilterExpression, FilterExpression> visitPredicate(FilterPredicate filterPredicate) {
        Type<?> tableType = filterPredicate.getFieldType();
        String fieldName = filterPredicate.getField();

        SQLTable table = (SQLTable) metaDataStore.getTable(tableType);

        Set<String> joins = lookupTable.getResolvedJoinExpressions(table, fieldName);

        if (joins.size() > 0) {
            return Pair.of(filterPredicate, null);
        } else {
            return Pair.of(null, filterPredicate);
        }
    }

    @Override
    public Pair<FilterExpression, FilterExpression> visitAndExpression(AndFilterExpression expression) {
        Pair<FilterExpression, FilterExpression> lhs = expression.getLeft().accept(this);
        Pair<FilterExpression, FilterExpression> rhs = expression.getLeft().accept(this);

        return Pair.of(
                AndFilterExpression.fromPair(lhs.getLeft(), rhs.getLeft()),
                AndFilterExpression.fromPair(lhs.getRight(), rhs.getRight())
        );
    }

    @Override
    public Pair<FilterExpression, FilterExpression> visitOrExpression(OrFilterExpression expression) {
        Pair<FilterExpression, FilterExpression> lhs = expression.getLeft().accept(this);
        Pair<FilterExpression, FilterExpression> rhs = expression.getLeft().accept(this);

        if (lhs.getLeft() != null || rhs.getLeft() != null) {
            FilterExpression combined = OrFilterExpression.fromPair(
                    AndFilterExpression.fromPair(lhs.getLeft(), lhs.getRight()),
                    AndFilterExpression.fromPair(rhs.getLeft(), rhs.getRight()));

            return Pair.of(combined, null);
        } else {
            return Pair.of(null,
                    OrFilterExpression.fromPair(lhs.getRight(), rhs.getRight())
            );
        }
    }

    @Override
    public Pair<FilterExpression, FilterExpression> visitNotExpression(NotFilterExpression expression) {
        Pair<FilterExpression, FilterExpression> inner = expression.getNegated().accept(this);

        return Pair.of(
                inner.getLeft() == null ? null : new NotFilterExpression(inner.getLeft()),
                inner.getRight() == null ? null : new NotFilterExpression(inner.getRight())
        );
    }
}
