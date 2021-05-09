/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
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
            return this;
        }

        if (keyStr.equals(ARGS_KEY)) {
            return this.column.getArguments();
        }

        // Keep Join References as is
        if (this.queryable.hasJoin(keyStr)) {
            return new StringValue(keyStr);
        }

        ColumnProjection column = this.queryable.getColumnProjection(keyStr);
        if (column != null) {

            ColumnProjection newColumn = column.withArguments(
                            getColumnArgMap(this.getQueryable(),
                                            column.getName(),
                                            this.getColumn().getArguments(),
                                            fixedArgs));

            return LogicalRefContext.builder()
                            .queryable(this.getQueryable())
                            .metaDataStore(this.getMetaDataStore())
                            .column(newColumn)
                            .build()
                            .resolve(newColumn.getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    @Override
    protected Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException {
        // Keep Join References as is
        if (!isBlank(options.hash("from"))) {
            return new StringValue(options.fn.text());
        }

        return super.resolveSQLHandlebar(context, options);
    }
}
