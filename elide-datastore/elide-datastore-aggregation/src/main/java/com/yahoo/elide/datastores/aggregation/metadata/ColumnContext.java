/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TemplateProjection;
import com.yahoo.elide.datastores.aggregation.query.TemplateProjection.TemplateProjectionBuilder;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Options;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.UnsupportedEncodingException;
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
            return getColArgMap(this.column);
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

            ColumnProjection newColumn = TemplateProjection.builder()
                            .name(columnProj.getName())
                            .expression(columnProj.getExpression())
                            .arguments(getColumnArgMap(this.getMetaDataStore(),
                                                       this.getQueryable(),
                                                       this.getQueriedColArgs(),
                                                       columnProj.getName(),
                                                       fixedArgs))
                            .build();

            ColumnContext newCtx = ColumnContext.builder()
                            .queryable(this.getQueryable())
                            .alias(this.getAlias())
                            .metaDataStore(this.getMetaDataStore())
                            .queriedColArgs(this.getQueriedColArgs())
                            .column(newColumn)
                            .build();

            return newCtx.resolve();
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    @Override
    public String resolve() {
        return resolveHandlebars(this, this.getColumn().getExpression());
    }

    @Override
    public String resolve(String expression) {

        ColumnContextBuilder ctxBuilder = ColumnContext.builder()
                        .queryable(this.getQueryable())
                        .alias(this.getAlias())
                        .metaDataStore(this.getMetaDataStore())
                        .queriedColArgs(this.getQueriedColArgs());

        TemplateProjectionBuilder columnBuilder = TemplateProjection.builder().expression(expression);

        ColumnProjection column = this.getColumn();
        ColumnProjection newColumn;
        if (column == null) {
            newColumn = columnBuilder
                            .arguments(this.getQueriedColArgs())
                            .build();
        } else {
            newColumn = columnBuilder
                            .name(column.getName())
                            .arguments(getColumnArgMap(this.getMetaDataStore(),
                                                       this.getQueryable(),
                                                       this.getQueriedColArgs(),
                                                       this.getColumn().getName(),
                                                       emptyMap()))
                            .build();
        }

        return ctxBuilder
                        .column(newColumn)
                        .build()
                        .resolve();
    }

    @Override
    protected Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException {
        String from = options.hash("from");
        String column = options.hash("column");
        int argsIndex = column.indexOf('[');
        String invokedColumnName = column;

        Context currentCtx = (Context) context;
        // 'from' is optional, so if not provided use the same table context.
        Context invokedCtx = isBlank(from) ? currentCtx
                                           : (Context) currentCtx.get(from);

        if (argsIndex >= 0) {
            Map<String, Argument> pinnedArgs = getArgumentMapFromString(column.substring(argsIndex));
            invokedColumnName = column.substring(0, argsIndex);
            return invokedCtx.get(invokedColumnName, pinnedArgs);
        }
        return invokedCtx.get(invokedColumnName);
    }
}
