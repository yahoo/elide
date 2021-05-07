/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.github.jknack.handlebars.HandlebarsException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * Context for resolving all handlebars in provided expression.
 */
@Getter
@ToString
@Builder
public class ColumnContext extends Context {

    private final MetaDataStore metaDataStore;
    private final Queryable queryable;
    private final String alias;

    // Arguments provided for queried column.
    private final Map<String, Argument> queriedColArgs;
    private final ColumnProjection column;

    @Override
    protected Object get(Object key, Map<String, Argument> fixedArgs) {

        String keyStr = key.toString();

        // Physical References starts with $
        if (keyStr.lastIndexOf('$') == 0) {
            String resolvedExpr = alias + PERIOD + key.toString().substring(1);
            return applyQuotes(resolvedExpr, queryable.getConnectionDetails().getDialect());
        }

        if (keyStr.equals(TBL_PREFIX)) {
            return getTableArgMap(this.queryable);
        }

        if (keyStr.equals(COL_PREFIX)) {
            return getColArgMap(this.column, this.queriedColArgs);
        }

        if (this.queryable.hasJoin(keyStr)) {
            SQLJoin sqlJoin = this.queryable.getJoin(keyStr);
            Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
            ColumnContext joinCtx = ColumnContext.builder()
                            .queryable(joinQueryable)
                            .alias(appendAlias(this.alias, keyStr))
                            .metaDataStore(this.metaDataStore)
                            .queriedColArgs(this.queriedColArgs)
                            .build();

            return joinCtx;
        }

        ColumnProjection columnProj = this.queryable.getColumnProjection(keyStr);
        if (columnProj != null) {

            ColumnProjection newColumn = columnProj.withArguments(getColumnArgMap(
                            this.getMetaDataStore(),
                            this.getQueryable(),
                            this.getQueriedColArgs(),
                            columnProj.getName(),
                            fixedArgs));

            return ColumnContext.builder()
                            .withColumn(this, newColumn)
                            .build()
                            .resolve();
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    public String resolve() {
        return resolve(this.getColumn().getExpression());
    }

    public static class ColumnContextBuilder {

        public ColumnContextBuilder withColumn(ColumnContext context, ColumnProjection column) {

            queryable(context.getQueryable());
            alias(context.getAlias());
            metaDataStore(context.getMetaDataStore());
            queriedColArgs(context.getQueriedColArgs());
            column(column);

            return this;
        }
    }
}
