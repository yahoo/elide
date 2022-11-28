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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * Context for resolving all handlebars in provided expression except physical references. Keeps physical references as
 * is.
 */
@Getter
@ToString
public class PhysicalRefColumnContext extends ColumnContext {

    private static final String HANDLEBAR_PREFIX = "{{";
    private static final String HANDLEBAR_SUFFIX = "}}";

    @Builder(builderMethodName = "physicalRefContextBuilder")
    public PhysicalRefColumnContext(MetaDataStore metaDataStore, Queryable queryable, String alias,
                    ColumnProjection column, Map<String, Argument> tableArguments) {
        super(metaDataStore, queryable, alias, column, tableArguments);
    }

    @Override
    protected ColumnContext getNewContext(ColumnContext context, ColumnProjection newColumn) {
        return PhysicalRefColumnContext.physicalRefContextBuilder()
                        .queryable(context.getQueryable())
                        .alias(context.getAlias())
                        .metaDataStore(context.getMetaDataStore())
                        .column(newColumn)
                        .tableArguments(context.getTableArguments())
                        .build();
    }

    @Override
    protected ColumnContext getJoinContext(String key) {
        SQLJoin sqlJoin = this.queryable.getJoin(key);
        Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
        String joinPath = isBlank(this.alias) ? key : this.alias + PERIOD + key;

        PhysicalRefColumnContext joinCtx = PhysicalRefColumnContext.physicalRefContextBuilder()
                        .queryable(joinQueryable)
                        .alias(joinPath)
                        .metaDataStore(this.metaDataStore)
                        .column(this.column)
                        .tableArguments(mergedArgumentMap(joinQueryable.getArguments(),
                                                          this.getTableArguments()))
                        .build();

        return joinCtx;
    }

    @Override
    protected String resolvePhysicalReference(ColumnContext context, String keyStr) {
        return isBlank(context.getAlias()) ? HANDLEBAR_PREFIX + keyStr + HANDLEBAR_SUFFIX
                                           : HANDLEBAR_PREFIX + context.getAlias() + PERIOD + keyStr + HANDLEBAR_SUFFIX;
    }
}
