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
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Options;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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
    private final Map<String, ? extends Object> queriedColArgs;
    private final Map<String, Object> columnArgsMap;

    @Override
    protected Object get(Object key, Map<String, ? extends Object> fixedArgs) {

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
            return this.columnArgsMap;
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
            return resolve(columnProj, fixedArgs);
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    @Override
    protected String resolve(ColumnProjection column, Map<String, ? extends Object> fixedArgs) {

        // Build a new Context for resolving this column
        ColumnContext newCtx = ColumnContext.builder()
                        .queryable(this.getQueryable())
                        .alias(this.getAlias())
                        .metaDataStore(this.getMetaDataStore())
                        .queriedColArgs(this.getQueriedColArgs())
                        .availableColArgs(getColumnArgMap(this.getMetaDataStore(),
                                                          this.getQueryable(),
                                                          this.getQueriedColArgs(),
                                                          column.getName(),
                                                          fixedArgs))
                        .build();

        com.github.jknack.handlebars.Context context = com.github.jknack.handlebars.Context.newBuilder(newCtx)
                        .resolver(new ContextResolver())
                        .build();
        return resolveHandlebars(context, column.getExpression());
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
            Map<String, ? extends Object> pinnedArgs = getArgumentMapFromString(column.substring(argsIndex));
            invokedColumnName = column.substring(0, argsIndex);
            return invokedCtx.get(invokedColumnName, pinnedArgs);
        }
        return invokedCtx.get(invokedColumnName);
    }

    public static class ColumnContextBuilder {

        public ColumnContextBuilder availableColArgs(final Map<String, Object> availableColArgs) {
            Map<String, Object> colArgsMap = new HashMap<>();
            colArgsMap.put(ARGS_KEY, availableColArgs);
            this.columnArgsMap = colArgsMap;
            return this;
        }
    }
}
