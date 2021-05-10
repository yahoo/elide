/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static org.apache.commons.lang3.StringUtils.isBlank;

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
 * Context for resolving arguments and logical references in column's expression. Keeps physical and join references as
 * is.
 */
@Getter
@ToString
@Builder
public class LogicalRefContext extends Context {

    private final MetaDataStore metaDataStore;
    private final Queryable queryable;
    private final String path;
    private final ColumnProjection column;

    @Override
    protected Object get(Object key, Map<String, Argument> fixedArgs) {

        String keyStr = key.toString();

        // Keep Physical References as is
        if (keyStr.lastIndexOf('$') == 0) {
            return isBlank(this.path) ? HANDLEBAR_PREFIX + keyStr + HANDLEBAR_SUFFIX
                                      : HANDLEBAR_PREFIX + this.path + PERIOD + keyStr + HANDLEBAR_SUFFIX;
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

        if (this.queryable.hasJoin(keyStr)) {
            SQLJoin sqlJoin = this.queryable.getJoin(keyStr);
            Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
            String joinPath = isBlank(this.path) ? keyStr : this.path + PERIOD + keyStr;

            LogicalRefContext joinCtx = LogicalRefContext.builder()
                            .queryable(joinQueryable)
                            .path(joinPath)
                            .metaDataStore(this.metaDataStore)
                            .column(this.column)
                            .build();

            return joinCtx;
        }

        // Check if key exists in Map.
        Object value = getOrDefault(key, null);
        if (value != null) {
            return value;
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
                            .path(this.getPath())
                            .column(newColumn)
                            .build()
                            .resolve(newColumn.getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }
}
