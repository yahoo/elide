/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.github.jknack.handlebars.HandlebarsException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TableContext for Handlebars Resolution.
 */
@Getter
@ToString
@Builder
public class TableContext extends HashMap<String, Object> {

    private final String alias;
    private final SQLDialect dialect;
    private final Map<String, Object> defaultTableArgs;
    @Builder.Default
    private final Map<String, TableContext> joins = new HashMap<>();
    @Builder.Default
    private final HandlebarResolver resolver = new HandlebarResolver();
    @Builder.Default
    private final List<TableContext> sourceCtx = new ArrayList<>();

    public Object get(Object key) {

        if (joins.containsKey(key)) {
            TableContext joinTblCtx = joins.get(key);
            TableContext newCtx = TableContext.builder()
                            .alias(appendAlias(alias, key.toString()))
                            .dialect(joinTblCtx.dialect)
                            .defaultTableArgs(joinTblCtx.defaultTableArgs)
                            .joins(joinTblCtx.joins)
                            .sourceCtx(joinTblCtx.sourceCtx)
                            .build();

            newCtx.putAll(joinTblCtx);
            return newCtx;
        }

        // Physical References starts with $
        if (key.toString().lastIndexOf('$') == 0) {
            String resolvedExpr = alias + PERIOD + key.toString().substring(1);
            return applyQuotes(resolvedExpr, dialect);
        }

        verifyKeyExists(key, super.keySet());

        Object value = super.get(key);
        if (value instanceof ColumnDefinition) {
            return resolver.resolveHandlebars(this, key.toString(), (ColumnDefinition) super.get(key));
        }
        return value;
    }

    public void addJoinContext(String joinName, TableContext joinCtx) {
        this.joins.put(joinName, joinCtx);
    }

    public void addSourceContext(TableContext sourceCtx) {
        this.sourceCtx.add(sourceCtx);
    }

    public TableContext getSourceContext() {
        if (this.sourceCtx.isEmpty()) {
            return null;
        }
        return this.sourceCtx.get(0);
    }

    /**
     * Gets the {@link ColumnDefinition} for provided column.
     * @param key Column Name
     * @return If key is a column name then returns {@link ColumnDefinition} else null.
     */
    public ColumnDefinition getColumnDefinition(String key) {
        Object value = super.get(key);
        if (value instanceof ColumnDefinition) {
            return (ColumnDefinition) value;
        }
        return null;
    }

    private void verifyKeyExists(Object key, Set<String> keySet) {
        if (!keySet.contains(key)) {
            throw new HandlebarsException(new Throwable("Couldn't find: " + key));
        }
    }
}
