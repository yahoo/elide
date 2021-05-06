/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TemplateProjection;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Options;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Context for resolving arguments and logical references in column's expression. Keeps physical and join references as
 * is.
 */
@Getter
@ToString
@Builder
public class LogicalRefContext extends Context {

    private final MetaDataStore metaDataStore;
    private final Queryable queryable;

    // Arguments provided for queried column.
    private final Map<String, Argument> queriedColArgs;
    private final ColumnProjection column;

    @Override
    protected Object get(Object key, Map<String, Argument> fixedArgs) {

        String keyStr = key.toString();

        // Keep Physical References as is
        if (keyStr.lastIndexOf('$') == 0) {
            return new StringValue(keyStr);
        }

        if (keyStr.equals(TBL_PREFIX)) {
            return getTableArgMap(this.queryable);
        }

        if (keyStr.equals(COL_PREFIX)) {
            return getColArgMap(this.column, this.queriedColArgs);
        }

        // Keep Join References as is
        if (this.queryable.hasJoin(keyStr)) {
            return new StringValue(keyStr);
        }

        ColumnProjection columnProj = this.queryable.getColumnProjection(keyStr);
        if (columnProj != null) {

            ColumnProjection newColumn = TemplateProjection.builder()
                            .expression(columnProj.getExpression())
                            .arguments(getColumnArgMap(this.getMetaDataStore(),
                                                       this.getQueryable(),
                                                       this.getQueriedColArgs(),
                                                       columnProj.getName(),
                                                       fixedArgs))
                            .build();

            return LogicalRefContext.builder()
                            .withColumn(this, newColumn)
                            .build()
                            .resolve();
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    public String resolve() {
        return resolve(this.getColumn().getExpression());
    }

    @Override
    protected Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException {
        String from = options.hash("from");
        String column = options.hash("column");
        int argsIndex = column.indexOf('[');
        String invokedColumnName = column;

        // Keep Join References as is
        if (!isBlank(from)) {
            return new StringValue(options.fn.text());
        }

        Context invokedCtx = (Context) context;

        if (argsIndex >= 0) {
            Map<String, Argument> pinnedArgs = getArgumentMapFromString(column.substring(argsIndex));
            invokedColumnName = column.substring(0, argsIndex);
            return invokedCtx.get(invokedColumnName, pinnedArgs);
        }

        return invokedCtx.get(invokedColumnName);
    }

    public static class LogicalRefContextBuilder {

        public LogicalRefContextBuilder withColumn(LogicalRefContext context, ColumnProjection column) {

            queryable(context.getQueryable());
            metaDataStore(context.getMetaDataStore());
            queriedColArgs(context.getQueriedColArgs());
            column(column);

            return this;
        }
    }
}
