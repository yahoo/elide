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
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            return getTableArgMap(this.queryable);
        }

        if (keyStr.equals(COL_PREFIX)) {
            return this;
        }

        if (keyStr.equals(ARGS_KEY)) {
            return this.column.getArguments();
        }

        if (keyStr.equals(EXPR_KEY)) {
            return this.resolve(this.getColumn().getExpression());
        }

        if (this.queryable.hasJoin(keyStr)) {
            return getJoinContext(keyStr);
        }

        // Check if key exists in Map.
        Object value = getOrDefault(key, null);
        if (value != null) {
            return value;
        }

        ColumnProjection column = this.getQueryable().getColumnProjection(keyStr);
        if (column != null) {

            ColumnProjection newColumn = column.withArguments(
                            getColumnArgMap(this.getQueryable(),
                                            column.getName(),
                                            this.getColumn().getArguments(),
                                            emptyMap()));

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
        Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
        ColumnContext joinCtx = ColumnContext.builder()
                        .queryable(joinQueryable)
                        .alias(appendAlias(this.alias, key))
                        .metaDataStore(this.metaDataStore)
                        .column(this.column)
                        .build();

        return joinCtx;
    }

    protected ColumnContext getNewContext(ColumnContext context, ColumnProjection newColumn) {
        return ColumnContext.builder()
                        .queryable(context.getQueryable())
                        .alias(context.getAlias())
                        .metaDataStore(context.getMetaDataStore())
                        .column(newColumn)
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

        ColumnProjection column = invokedCtx.getQueryable().getColumnProjection(invokedColumnName);
        if (column != null) {

            ColumnProjection newColumn = column.withArguments(
                            getColumnArgMap(invokedCtx.getQueryable(),
                                            column.getName(),
                                            invokedCtx.getColumn().getArguments(),
                                            pinnedArgs));

            return getNewContext(invokedCtx, newColumn).resolve(newColumn.getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + invokedColumnName));
    }

    public static Map<String, Argument> getDefaultArgumentsMap(
                    Set<com.yahoo.elide.datastores.aggregation.metadata.models.Argument> availableArgs) {

         return availableArgs.stream()
                        .filter(arg -> arg.getDefaultValue() != null)
                        .map(arg -> Argument.builder()
                                        .name(arg.getName())
                                        .value(arg.getDefaultValue())
                                        .build())
                        .collect(Collectors.toMap(Argument::getName, Function.identity()));
    }

    protected static Map<String, Object> getTableArgMap(Queryable queryable) {
        Map<String, Object> tblArgsMap = new HashMap<>();

        if (queryable instanceof SQLTable) {
            SQLTable table = (SQLTable) queryable;
            tblArgsMap.put(ARGS_KEY, getDefaultArgumentsMap(table.getArguments()));
            return tblArgsMap;
        }

        Query query = (Query) queryable;
        tblArgsMap.put(ARGS_KEY, query.getArguments());
        return tblArgsMap;
    }

    public static Map<String, Argument> getColumnArgMap(Queryable queryable,
                                                        String columnName,
                                                        Map<String, Argument> callingColumnArgs,
                                                        Map<String, Argument> fixedArgs) {
        return getColumnArgMap(queryable.getSource().getColumnProjection(columnName).getArguments(),
                               callingColumnArgs,
                               fixedArgs);
    }

    public static Map<String, Argument> getColumnArgMap(Map<String, Argument> referencedColumnArgs,
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
}
