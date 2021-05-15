/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.query.ColumnProjection.createSafeAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Formatter;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Context for resolving all handlebars in provided expression.
 */
@Getter
@ToString
@Builder
public class ColumnContext extends HashMap<String, Object> {

    public static final String COL_PREFIX = "$$column";
    public static final String TBL_PREFIX = "$$table";
    public static final String ARGS_KEY = "args";
    public static final String EXPR_KEY = "expr";

    protected final MetaDataStore metaDataStore;
    protected final Queryable queryable;
    protected final String alias;
    protected final ColumnProjection column;
    protected final Map<String, Argument> tableArguments;

    private final Handlebars handlebars = new Handlebars()
            .with(EscapingStrategy.NOOP)
            .with((Formatter) (value, next) -> {
                if (value instanceof com.yahoo.elide.core.request.Argument) {
                    return ((com.yahoo.elide.core.request.Argument) value).getValue();
                }
                return next.format(value);
            })
            .registerHelper("sql", this::resolveSQLHandlebar);

    public Object get(Object key) {

        String keyStr = key.toString();

        // Physical References starts with $
        if (keyStr.lastIndexOf('$') == 0) {
            return resolvePhysicalReference(this, keyStr);
        }

        if (keyStr.equals(TBL_PREFIX)) {
            return TableSubContext.tableSubContextBuilder()
                            .tableArguments(this.tableArguments)
                            .build();
        }

        if (keyStr.equals(COL_PREFIX)) {
            return ColumnSubContext.columnSubContextBuilder()
                            .queryable(this.getQueryable())
                            .alias(this.getAlias())
                            .metaDataStore(this.getMetaDataStore())
                            .column(this.getColumn())
                            .tableArguments(this.getTableArguments())
                            .build();
        }

        if (this.queryable.hasJoin(keyStr)) {
            return getJoinContext(keyStr);
        }

        // Check if key exists in Map.
        Object value = getOrDefault(key, null);
        if (value != null) {
            return value;
        }

        ColumnProjection column = this.getQueryable().getSource().getColumnProjection(keyStr);
        if (column != null) {

            ColumnProjection newColumn = column.withArguments(
                            mergedArgumentMap(column.getArguments(),
                                              this.getColumn().getArguments()));

            return getNewContext(this, newColumn).resolve(newColumn.getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + keyStr));
    }

    protected String resolvePhysicalReference(ColumnContext context, String key) {
        String resolvedExpr = context.getAlias() + PERIOD + key.substring(1);
        return applyQuotes(resolvedExpr, context.getQueryable().getDialect());
    }

    protected ColumnContext getJoinContext(String key) {
        SQLJoin sqlJoin = this.queryable.getJoin(key);

        String joinExpression = sqlJoin.getJoinExpression();
        PhysicalRefColumnContext context = PhysicalRefColumnContext.physicalRefContextBuilder()
                        .queryable(this.getQueryable())
                        .metaDataStore(this.getMetaDataStore())
                        .column(this.getColumn())
                        .tableArguments(this.getTableArguments())
                        .build();

        // This will resolve everything within join expression except Physical References, Use this resolved value
        // to create alias for join dynamically.
        String resolvedJoinExpr = context.resolve(joinExpression);
        String joinAlias = createSafeAlias(appendAlias(this.alias, key), resolvedJoinExpr);

        Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
        ColumnContext joinCtx = ColumnContext.builder()
                        .queryable(joinQueryable)
                        .alias(joinAlias)
                        .metaDataStore(this.metaDataStore)
                        .column(this.column)
                        .tableArguments(mergedArgumentMap(joinQueryable.getArguments(),
                                                          this.getTableArguments()))
                        .build();

        return joinCtx;
    }

    protected ColumnContext getNewContext(ColumnContext context, ColumnProjection newColumn) {
        return ColumnContext.builder()
                        .queryable(context.getQueryable())
                        .alias(context.getAlias())
                        .metaDataStore(context.getMetaDataStore())
                        .column(newColumn)
                        .tableArguments(context.getTableArguments())
                        .build();
    }

    /**
     * Resolves the handlebars with in expression.
     * @param expression Expression to resolve.
     * @return fully resolved expression.
     * @throws IllegalStateException for any handlebars error.
     */
    public String resolve(String expression) {
        try {
            Template template = handlebars.compileInline(expression);
            return template.apply(this);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException {
        String from = options.hash("from");
        String columnName = options.hash("column");
        int argsIndex = columnName.indexOf('[');
        String invokedColumnName = columnName;

        ColumnContext currentCtx = (ColumnContext) context;
        // 'from' is optional, so if not provided use the same table context.
        ColumnContext invokedCtx = isBlank(from) ? currentCtx
                                                 : (ColumnContext) currentCtx.get(from);

        Map<String, Argument> pinnedArgs = new HashMap<>();

        if (argsIndex >= 0) {
            pinnedArgs = getArgumentMapFromString(columnName.substring(argsIndex));
            invokedColumnName = columnName.substring(0, argsIndex);
        }

        // Physical References starts with $
        if (invokedColumnName.lastIndexOf('$') == 0) {
            return resolvePhysicalReference(invokedCtx, invokedColumnName);
        }

        ColumnProjection column = invokedCtx.getQueryable().getSource().getColumnProjection(invokedColumnName);
        if (column != null) {

            ColumnProjection newColumn = column.withArguments(
                            mergedArgumentMap(column.getArguments(),
                                              invokedCtx.getColumn().getArguments(),
                                              pinnedArgs));

            return getNewContext(invokedCtx, newColumn).resolve(newColumn.getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + invokedColumnName));
    }

    public static Map<String, Argument> mergedArgumentMap(Map<String, Argument> referencedColumnArgs,
                                                          Map<String, Argument> callingColumnArgs,
                                                          Map<String, Argument> fixedArgs) {

        Map<String, Argument> columnArgMap = new HashMap<>();

        referencedColumnArgs.forEach((argName, arg) -> {
            if (fixedArgs.containsKey(argName)) {
                columnArgMap.put(argName, fixedArgs.get(argName));
            } else if (callingColumnArgs.containsKey(argName)) {
                columnArgMap.put(argName, callingColumnArgs.get(argName));
            } else {
                columnArgMap.put(argName, arg);
            }
        });

        return columnArgMap;
    }

    public static Map<String, Argument> mergedArgumentMap(Map<String, Argument> referencedColumnArgs,
                    Map<String, Argument> callingColumnArgs) {
        return mergedArgumentMap(referencedColumnArgs, callingColumnArgs, emptyMap());
    }
}
