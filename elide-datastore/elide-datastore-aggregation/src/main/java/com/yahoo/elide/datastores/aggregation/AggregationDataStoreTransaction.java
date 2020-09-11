/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.core.QueryResponse;
import com.yahoo.elide.datastores.aggregation.filter.visitor.MatchesTemplateVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.request.EntityProjection;

import com.google.common.annotations.VisibleForTesting;

import lombok.ToString;

import java.io.IOException;

/**
 * Transaction handler for {@link AggregationDataStore}.
 */
@ToString
public abstract class AggregationDataStoreTransaction implements DataStoreTransaction {
    protected final QueryEngine queryEngine;
    protected final Cache cache;
    protected QueryEngine.Transaction queryEngineTransaction;
    protected final QueryLogger queryLogger;

    public AggregationDataStoreTransaction(QueryEngine queryEngine, Cache cache,
                                           QueryLogger queryLogger) {
        this.queryEngine = queryEngine;
        this.cache = cache;
        this.queryLogger = queryLogger;
    }

    @Override
    public void save(Object entity, RequestScope scope) {

    }

    @Override
    public void delete(Object entity, RequestScope scope) {

    }

    @Override
    public void flush(RequestScope scope) {

    }

    @Override
    public void commit(RequestScope scope) {
        if (queryEngineTransaction != null) {
            queryEngineTransaction.close();
        }
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {

    }

    @Override
    public abstract Iterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope);

    @Override
    public void close() throws IOException {
        if (queryEngineTransaction != null) {
            queryEngineTransaction.close();
        }
    }

    @VisibleForTesting
    protected Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
        Table table = queryEngine.getTable(scope.getDictionary().getJsonAliasFor(entityProjection.getType()));
        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                queryEngine,
                table,
                entityProjection,
                scope.getDictionary());
        Query query = translator.getQuery();

        FilterExpression filterTemplate = table.getRequiredFilter(scope.getDictionary());
        if (filterTemplate != null && ! MatchesTemplateVisitor.isValid(filterTemplate, query.getWhereFilter())) {
            String message = String.format("Querying %s requires a mandatory filter: %s",
                        table.getName(), table.getRequiredFilter().toString());

            throw new BadRequestException(message);
        }

        return query;
    }

    @Override
    public void cancel(RequestScope scope) {
        queryLogger.cancelQuery(scope.getRequestId());
        if (queryEngineTransaction != null) {
            queryEngineTransaction.cancel();
        }
    }
}
