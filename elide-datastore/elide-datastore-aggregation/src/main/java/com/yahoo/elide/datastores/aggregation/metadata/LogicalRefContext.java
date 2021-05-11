/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Context for resolving arguments and logical references in column's expression. Keeps physical and join references as
 * is.
 */
@Getter
@ToString
public class LogicalRefContext extends ColumnContext {

    private static final String HANDLEBAR_PREFIX = "{{";
    private static final String HANDLEBAR_SUFFIX = "}}";

    @Builder(builderMethodName = "logicalRefContextBuilder")
    public LogicalRefContext(MetaDataStore metaDataStore, Queryable queryable, String alias, ColumnProjection column) {
        super(metaDataStore, queryable, alias, column);
    }

    @Override
    protected ColumnContext getNewContext(ColumnProjection newColumn) {
        return LogicalRefContext.logicalRefContextBuilder()
                        .queryable(this.getQueryable())
                        .alias(this.getAlias())
                        .metaDataStore(this.getMetaDataStore())
                        .column(newColumn)
                        .build();
    }

    @Override
    protected ColumnContext getJoinContext(String key) {
        SQLJoin sqlJoin = this.queryable.getJoin(key);
        Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
        String joinPath = isBlank(this.alias) ? key : this.alias + PERIOD + key;

        LogicalRefContext joinCtx = LogicalRefContext.logicalRefContextBuilder()
                        .queryable(joinQueryable)
                        .alias(joinPath)
                        .metaDataStore(this.metaDataStore)
                        .column(this.column)
                        .build();

        return joinCtx;
    }

    @Override
    protected String resolvePhysicalReference(String keyStr) {
        return isBlank(this.alias) ? HANDLEBAR_PREFIX + keyStr + HANDLEBAR_SUFFIX
                                   : HANDLEBAR_PREFIX + this.alias + PERIOD + keyStr + HANDLEBAR_SUFFIX;
    }
}
