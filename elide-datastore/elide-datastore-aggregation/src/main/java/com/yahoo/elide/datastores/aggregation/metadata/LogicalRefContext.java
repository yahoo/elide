/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TemplateProjection;
import com.yahoo.elide.datastores.aggregation.query.TemplateProjection.TemplateProjectionBuilder;
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
            return getColArgMap(this.column);
        }

        // Keep Join References as is
        if (this.queryable.hasJoin(keyStr)) {
            return new StringValue(keyStr);
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

            LogicalRefContext newCtx = LogicalRefContext.builder()
                            .queryable(this.getQueryable())
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

        return LogicalRefContext.builder()
                        .queryable(this.getQueryable())
                        .metaDataStore(this.getMetaDataStore())
                        .queriedColArgs(this.getQueriedColArgs())
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

        // Keep Join References as is
        if (!isBlank(from)) {
            return new StringValue(options.fn.text());
        }

        LogicalRefContext invokedCtx = (LogicalRefContext) context;

        if (argsIndex >= 0) {
            Map<String, Argument> pinnedArgs = getArgumentMapFromString(column.substring(argsIndex));
            invokedColumnName = column.substring(0, argsIndex);
            return invokedCtx.get(invokedColumnName, pinnedArgs);
        }

        return invokedCtx.get(invokedColumnName);
    }
}
